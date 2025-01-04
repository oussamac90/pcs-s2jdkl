import { Injectable, OnDestroy } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { 
  BehaviorSubject, 
  Observable, 
  Subject, 
  timer, 
  of, 
  throwError,
  Subscription 
} from 'rxjs';
import { 
  takeUntil, 
  filter, 
  retry, 
  catchError, 
  timeout, 
  delay, 
  tap 
} from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// WebSocket event types based on system requirements
export enum WebSocketEventType {
  VESSEL_UPDATE = 'VESSEL_UPDATE',
  BERTH_CHANGE = 'BERTH_CHANGE',
  SERVICE_STATUS = 'SERVICE_STATUS',
  CLEARANCE_UPDATE = 'CLEARANCE_UPDATE'
}

// Connection status states for detailed monitoring
export enum ConnectionStatus {
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  ERROR = 'ERROR',
  RECONNECTING = 'RECONNECTING'
}

// Error types for specific handling strategies
export enum WebSocketErrorType {
  CONNECTION_ERROR = 'CONNECTION_ERROR',
  SUBSCRIPTION_ERROR = 'SUBSCRIPTION_ERROR',
  MESSAGE_ERROR = 'MESSAGE_ERROR',
  TIMEOUT_ERROR = 'TIMEOUT_ERROR'
}

// Type-safe message interface
export interface WebSocketMessage<T> {
  type: WebSocketEventType;
  payload: T;
  timestamp: Date;
  id: string;
  status: string;
}

// Configuration interface
export interface WebSocketConfig {
  reconnectInterval: number;
  maxReconnectAttempts: number;
  heartbeatInterval: number;
  connectionTimeout: number;
}

// Constants
const RECONNECT_INTERVAL = 5000;
const MAX_RECONNECT_ATTEMPTS = 5;
const HEARTBEAT_INTERVAL = 30000;
const CONNECTION_TIMEOUT = 10000;
const MESSAGE_BUFFER_SIZE = 100;

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private stompClient: Client;
  private connectionStatus$ = new BehaviorSubject<ConnectionStatus>(ConnectionStatus.DISCONNECTED);
  private messageSubject$ = new BehaviorSubject<WebSocketMessage<any>>(null);
  private destroy$ = new Subject<void>();
  private reconnectAttempts = 0;
  private subscriptions = new Map<string, Subscription>();
  private config: WebSocketConfig;

  constructor() {
    this.initializeConfig();
    this.initializeStompClient();
    this.setupHeartbeat();
  }

  private initializeConfig(): void {
    this.config = {
      reconnectInterval: RECONNECT_INTERVAL,
      maxReconnectAttempts: MAX_RECONNECT_ATTEMPTS,
      heartbeatInterval: HEARTBEAT_INTERVAL,
      connectionTimeout: CONNECTION_TIMEOUT
    };
  }

  private initializeStompClient(): void {
    this.stompClient = new Client({
      brokerURL: environment.wsUrl,
      debug: (str) => {
        if (!environment.production) {
          console.debug(str);
        }
      },
      reconnectDelay: this.config.reconnectInterval,
      heartbeatIncoming: this.config.heartbeatInterval,
      heartbeatOutgoing: this.config.heartbeatInterval
    });
  }

  private setupHeartbeat(): void {
    timer(0, this.config.heartbeatInterval)
      .pipe(
        takeUntil(this.destroy$),
        filter(() => this.stompClient?.connected)
      )
      .subscribe(() => {
        this.stompClient.publish({
          destination: '/app/heartbeat',
          body: JSON.stringify({ timestamp: new Date() })
        });
      });
  }

  public async connect(): Promise<void> {
    if (this.stompClient.connected) {
      return Promise.resolve();
    }

    this.connectionStatus$.next(ConnectionStatus.CONNECTING);

    return new Promise((resolve, reject) => {
      this.stompClient.onConnect = () => {
        this.connectionStatus$.next(ConnectionStatus.CONNECTED);
        this.reconnectAttempts = 0;
        resolve();
      };

      this.stompClient.onStompError = (error) => {
        this.handleError(WebSocketErrorType.CONNECTION_ERROR, error);
        reject(error);
      };

      this.stompClient.onWebSocketError = (error) => {
        this.handleError(WebSocketErrorType.CONNECTION_ERROR, error);
        this.attemptReconnection();
        reject(error);
      };

      try {
        this.stompClient.activate();
      } catch (error) {
        this.handleError(WebSocketErrorType.CONNECTION_ERROR, error);
        reject(error);
      }
    }).pipe(
      timeout(this.config.connectionTimeout),
      catchError((error) => {
        this.handleError(WebSocketErrorType.TIMEOUT_ERROR, error);
        return throwError(() => error);
      })
    ) as Promise<void>;
  }

  private attemptReconnection(): void {
    if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
      this.connectionStatus$.next(ConnectionStatus.ERROR);
      return;
    }

    this.reconnectAttempts++;
    this.connectionStatus$.next(ConnectionStatus.RECONNECTING);

    timer(this.config.reconnectInterval * Math.pow(2, this.reconnectAttempts - 1))
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.connect().catch(() => {
          this.attemptReconnection();
        });
      });
  }

  public disconnect(): void {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
    this.subscriptions.clear();

    if (this.stompClient?.connected) {
      this.stompClient.deactivate();
    }

    this.connectionStatus$.next(ConnectionStatus.DISCONNECTED);
    this.reconnectAttempts = 0;
  }

  public subscribe<T>(topic: string, eventType: WebSocketEventType): Observable<WebSocketMessage<T>> {
    if (!this.stompClient?.connected) {
      return throwError(() => new Error('WebSocket not connected'));
    }

    const subscription = this.stompClient.subscribe(topic, (message) => {
      try {
        const parsedMessage: WebSocketMessage<T> = {
          type: eventType,
          payload: JSON.parse(message.body),
          timestamp: new Date(),
          id: message.headers['message-id'],
          status: 'RECEIVED'
        };
        this.messageSubject$.next(parsedMessage);
      } catch (error) {
        this.handleError(WebSocketErrorType.MESSAGE_ERROR, error);
      }
    }, {
      id: `sub-${topic}-${Date.now()}`,
      ack: 'client-individual'
    });

    this.subscriptions.set(topic, subscription);

    return this.messageSubject$.pipe(
      filter((message): message is WebSocketMessage<T> => 
        message?.type === eventType && message !== null
      ),
      takeUntil(this.destroy$),
      catchError((error) => {
        this.handleError(WebSocketErrorType.SUBSCRIPTION_ERROR, error);
        return throwError(() => error);
      })
    );
  }

  public getConnectionStatus(): Observable<ConnectionStatus> {
    return this.connectionStatus$.asObservable();
  }

  private handleError(errorType: WebSocketErrorType, error: any): void {
    console.error(`WebSocket ${errorType}:`, error);

    // Log to monitoring system if in production
    if (environment.production && environment.monitoring?.enabled) {
      // Implementation would depend on monitoring service integration
      this.logToMonitoring(errorType, error);
    }

    if (errorType === WebSocketErrorType.CONNECTION_ERROR) {
      this.attemptReconnection();
    }
  }

  private logToMonitoring(errorType: WebSocketErrorType, error: any): void {
    // Example monitoring integration
    const errorDetails = {
      type: errorType,
      message: error.message,
      timestamp: new Date(),
      reconnectAttempts: this.reconnectAttempts,
      connectionStatus: this.connectionStatus$.value
    };

    // Monitoring service call would go here
    console.warn('Monitoring:', errorDetails);
  }

  public ngOnDestroy(): void {
    this.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
    this.messageSubject$.complete();
    this.connectionStatus$.complete();
  }
}
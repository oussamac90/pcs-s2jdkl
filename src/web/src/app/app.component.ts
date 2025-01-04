import { Component, OnInit, OnDestroy, ErrorHandler } from '@angular/core';
import { Subject, BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, catchError } from 'rxjs/operators';

import { LoadingService } from './core/services/loading.service';
import { NotificationService } from './core/services/notification.service';
import { WebSocketService, ConnectionStatus } from './core/services/websocket.service';

/**
 * Root component of the Vessel Call Management System that provides the main application shell
 * and manages global application state including loading indicators, real-time updates,
 * and notifications.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  /** Title of the application */
  readonly title = 'Vessel Call Management System';

  /** Subject for managing component subscriptions */
  private readonly destroy$ = new Subject<void>();

  /** Observable of the global loading state */
  isLoading$: Observable<boolean>;

  /** Observable of WebSocket connection state */
  wsConnection$: Observable<ConnectionStatus>;

  /** Flag to track offline status */
  private isOffline = false;

  /** Debounce time for loading state changes in milliseconds */
  private readonly LOADING_DEBOUNCE_TIME = 200;

  constructor(
    private loadingService: LoadingService,
    private notificationService: NotificationService,
    private wsService: WebSocketService
  ) {
    // Initialize loading state with debounce to prevent flickering
    this.isLoading$ = this.loadingService.isLoading$.pipe(
      debounceTime(this.LOADING_DEBOUNCE_TIME),
      distinctUntilChanged(),
      catchError(error => {
        console.error('Loading state error:', error);
        return new BehaviorSubject(false);
      })
    );

    // Initialize WebSocket connection state
    this.wsConnection$ = this.wsService.getConnectionStatus();
  }

  async ngOnInit(): Promise<void> {
    // Enable notifications with offline support
    this.notificationService.enableNotifications();

    // Initialize WebSocket connection
    try {
      await this.wsService.connect();
    } catch (error) {
      console.error('WebSocket connection error:', error);
    }

    // Monitor WebSocket connection status
    this.wsConnection$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        switch (status) {
          case ConnectionStatus.CONNECTED:
            this.handleConnectionEstablished();
            break;
          case ConnectionStatus.DISCONNECTED:
          case ConnectionStatus.ERROR:
            this.handleConnectionLost();
            break;
          case ConnectionStatus.RECONNECTING:
            this.handleReconnecting();
            break;
        }
      });

    // Monitor offline status
    window.addEventListener('online', () => this.handleOnlineStatus(true));
    window.addEventListener('offline', () => this.handleOnlineStatus(false));
  }

  ngOnDestroy(): void {
    // Clean up subscriptions and connections
    this.destroy$.next();
    this.destroy$.complete();
    
    // Disconnect WebSocket
    this.wsService.disconnect();
    
    // Remove event listeners
    window.removeEventListener('online', () => this.handleOnlineStatus(true));
    window.removeEventListener('offline', () => this.handleOnlineStatus(false));
  }

  /**
   * Handles successful WebSocket connection establishment
   */
  private handleConnectionEstablished(): void {
    this.notificationService.showNotification(
      'Real-time connection established',
      'SUCCESS',
      { duration: 3000, sound: false }
    );
  }

  /**
   * Handles WebSocket connection loss
   */
  private handleConnectionLost(): void {
    this.notificationService.showNotification(
      'Real-time connection lost. Some features may be limited.',
      'WARNING',
      { persistent: true, sound: true }
    );
  }

  /**
   * Handles WebSocket reconnection attempts
   */
  private handleReconnecting(): void {
    this.notificationService.showNotification(
      'Attempting to restore connection...',
      'INFO',
      { duration: 2000, sound: false }
    );
  }

  /**
   * Handles changes in online/offline status
   * @param isOnline Current online status
   */
  private handleOnlineStatus(isOnline: boolean): void {
    this.isOffline = !isOnline;
    
    if (isOnline) {
      this.notificationService.showNotification(
        'Connection restored',
        'SUCCESS',
        { duration: 3000 }
      );
      this.wsService.connect().catch(console.error);
    } else {
      this.notificationService.showNotification(
        'You are offline. Some features may be unavailable.',
        'WARNING',
        { persistent: true }
      );
    }
  }
}
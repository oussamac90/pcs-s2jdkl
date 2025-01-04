import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';
import { Subject, BehaviorSubject, Observable, ReplaySubject } from 'rxjs';
import { takeUntil, filter, concatMap, delay } from 'rxjs/operators';
import { WebSocketService, WebSocketEventType } from './websocket.service';
import { ErrorService } from './error.service';

export enum NotificationType {
  SUCCESS = 'SUCCESS',
  ERROR = 'ERROR',
  WARNING = 'WARNING',
  INFO = 'INFO'
}

export interface NotificationConfig {
  duration: number;
  action: string;
  panelClass: string[];
  priority: number;
  persistent: boolean;
  sound: boolean;
  ariaLive: 'polite' | 'assertive' | 'off';
}

const DEFAULT_NOTIFICATION_DURATION = 5000;
const DEFAULT_NOTIFICATION_ACTION = 'Close';
const NOTIFICATION_QUEUE_DELAY = 300;
const MAX_NOTIFICATION_HISTORY = 50;

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsEnabled = new BehaviorSubject<boolean>(true);
  private destroy$ = new Subject<void>();
  private notificationQueue = new ReplaySubject<NotificationConfig>(10);
  private notificationHistory = new Map<string, number>();
  
  private defaultConfig: NotificationConfig = {
    duration: DEFAULT_NOTIFICATION_DURATION,
    action: DEFAULT_NOTIFICATION_ACTION,
    panelClass: [],
    priority: 3,
    persistent: false,
    sound: true,
    ariaLive: 'polite'
  };

  constructor(
    private snackBar: MatSnackBar,
    private wsService: WebSocketService,
    private errorService: ErrorService
  ) {
    this.setupWebSocketNotifications();
    this.processNotificationQueue();
  }

  private setupWebSocketNotifications(): void {
    // Subscribe to vessel updates
    this.wsService.subscribe<any>('/topic/vessel-updates', WebSocketEventType.VESSEL_UPDATE)
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        this.showNotification(
          `Vessel ${message.payload.vesselName} status updated to ${message.payload.status}`,
          NotificationType.INFO
        );
      });

    // Subscribe to berth changes
    this.wsService.subscribe<any>('/topic/berth-updates', WebSocketEventType.BERTH_CHANGE)
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        this.showNotification(
          `Berth allocation changed for ${message.payload.berthId}`,
          NotificationType.WARNING,
          { priority: 4, sound: true }
        );
      });

    // Subscribe to service status updates
    this.wsService.subscribe<any>('/topic/service-updates', WebSocketEventType.SERVICE_STATUS)
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        this.showNotification(
          `Service ${message.payload.serviceId} status: ${message.payload.status}`,
          NotificationType.INFO
        );
      });

    // Subscribe to clearance updates
    this.wsService.subscribe<any>('/topic/clearance-updates', WebSocketEventType.CLEARANCE_UPDATE)
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        this.showNotification(
          `Clearance update: ${message.payload.message}`,
          NotificationType.SUCCESS,
          { priority: 5, persistent: true }
        );
      });
  }

  private processNotificationQueue(): void {
    this.notificationQueue
      .pipe(
        takeUntil(this.destroy$),
        concatMap(config => {
          return new Observable(observer => {
            const snackBarRef = this.snackBar.open(
              config['message'] as string,
              config.action,
              {
                ...config,
                panelClass: [...config.panelClass, 'notification-snackbar']
              }
            );

            if (config.sound) {
              this.playNotificationSound(config.priority);
            }

            snackBarRef.afterDismissed().subscribe(() => {
              observer.next();
              observer.complete();
            });
          }).pipe(delay(NOTIFICATION_QUEUE_DELAY));
        })
      )
      .subscribe();
  }

  public showNotification(
    message: string,
    type: NotificationType,
    config?: Partial<NotificationConfig>
  ): void {
    if (!this.notificationsEnabled.value) {
      return;
    }

    // Check for duplicate notifications
    const notificationKey = `${message}-${type}`;
    const lastShown = this.notificationHistory.get(notificationKey);
    if (lastShown && Date.now() - lastShown < 1000) {
      return;
    }

    const finalConfig: NotificationConfig = {
      ...this.defaultConfig,
      ...config,
      panelClass: [
        `notification-${type.toLowerCase()}`,
        ...(config?.panelClass || [])
      ],
      ariaLive: type === NotificationType.ERROR ? 'assertive' : 'polite'
    };

    this.notificationQueue.next({
      ...finalConfig,
      message
    } as NotificationConfig);

    // Update notification history
    this.notificationHistory.set(notificationKey, Date.now());
    if (this.notificationHistory.size > MAX_NOTIFICATION_HISTORY) {
      const oldestKey = this.notificationHistory.keys().next().value;
      this.notificationHistory.delete(oldestKey);
    }
  }

  private playNotificationSound(priority: number): void {
    if (!this.notificationsEnabled.value) {
      return;
    }

    const audio = new Audio();
    audio.src = priority >= 4 ? 'assets/sounds/high-priority.mp3' : 'assets/sounds/notification.mp3';
    audio.play().catch(() => {
      console.warn('Failed to play notification sound');
    });
  }

  public enableNotifications(): void {
    this.notificationsEnabled.next(true);
  }

  public disableNotifications(): void {
    this.notificationsEnabled.next(false);
  }

  public ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.notificationQueue.complete();
    this.notificationsEnabled.complete();
  }
}
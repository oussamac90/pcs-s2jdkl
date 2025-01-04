import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { MatBadgeModule, MatMenuModule, MatIconModule, MatTooltipModule } from '@angular/material';

import { User } from '../../core/auth/user.model';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationService, NotificationType } from '../../core/services/notification.service';

/**
 * Header component that implements the main navigation bar of the VCMS.
 * Provides user profile management, real-time notifications, and session management.
 */
@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  notificationCount = 0;
  showNotifications = false;
  showSessionTimeout = false;
  sessionTimeoutCounter = 0;
  notifications: any[] = [];
  isOffline = !navigator.onLine;

  private readonly SESSION_WARNING_THRESHOLD = 300; // 5 minutes
  private readonly NOTIFICATION_DEBOUNCE = 500; // milliseconds
  private destroy$ = new Subject<void>();
  private subscriptions: Subscription[] = [];

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router
  ) {
    // Initialize offline detection
    window.addEventListener('online', this.handleConnectionChange.bind(this));
    window.addEventListener('offline', this.handleConnectionChange.bind(this));
  }

  ngOnInit(): void {
    // Subscribe to user authentication state
    this.subscriptions.push(
      this.authService.currentUser$.subscribe(user => {
        this.currentUser = user;
        if (user) {
          this.initializeNotifications();
        }
      })
    );

    // Subscribe to notification updates with debounce
    this.subscriptions.push(
      this.notificationService.notifications$
        .pipe(debounceTime(this.NOTIFICATION_DEBOUNCE))
        .subscribe(notifications => {
          this.notifications = this.sortNotificationsByPriority(notifications);
          this.updateNotificationCount();
          this.handleHighPriorityNotifications(notifications);
        })
    );

    // Subscribe to session timeout warnings
    this.subscriptions.push(
      this.authService.sessionTimeout$.subscribe(timeLeft => {
        this.handleSessionTimeout(timeLeft);
      })
    );
  }

  ngOnDestroy(): void {
    // Clean up subscriptions and event listeners
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.destroy$.next();
    this.destroy$.complete();
    window.removeEventListener('online', this.handleConnectionChange.bind(this));
    window.removeEventListener('offline', this.handleConnectionChange.bind(this));
  }

  /**
   * Handles user logout with cleanup
   */
  async onLogout(): Promise<void> {
    try {
      await this.authService.logout();
      this.notificationService.showNotification(
        'You have been successfully logged out',
        NotificationType.INFO
      );
      this.router.navigate(['/login']);
    } catch (error) {
      this.notificationService.showNotification(
        'Error during logout. Please try again.',
        NotificationType.ERROR
      );
    }
  }

  /**
   * Toggles notification panel visibility
   */
  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.notificationService.markAsRead(this.notifications.map(n => n.id));
      this.updateNotificationCount();
    }
  }

  /**
   * Handles online/offline status changes
   */
  private handleConnectionChange(event: Event): void {
    this.isOffline = !navigator.onLine;
    const message = this.isOffline 
      ? 'You are currently offline. Some features may be unavailable.'
      : 'Connection restored.';
    const type = this.isOffline ? NotificationType.WARNING : NotificationType.SUCCESS;
    this.notificationService.showNotification(message, type);
  }

  /**
   * Initializes notification handling
   */
  private initializeNotifications(): void {
    if (!this.currentUser) return;

    // Subscribe to vessel updates based on user role
    if (this.authService.hasAnyRole(['PORT_AUTHORITY', 'VESSEL_AGENT'])) {
      this.notificationService.enableNotifications();
    }
  }

  /**
   * Sorts notifications by priority
   */
  private sortNotificationsByPriority(notifications: any[]): any[] {
    return [...notifications].sort((a, b) => b.priority - a.priority);
  }

  /**
   * Updates notification badge count
   */
  private updateNotificationCount(): void {
    this.notificationCount = this.notifications.filter(n => !n.read).length;
  }

  /**
   * Handles high-priority notifications with sound alerts
   */
  private handleHighPriorityNotifications(notifications: any[]): void {
    const highPriorityNotifications = notifications.filter(n => n.priority >= 4 && !n.read);
    if (highPriorityNotifications.length > 0) {
      this.notificationService.playNotificationSound();
    }
  }

  /**
   * Handles session timeout warnings
   */
  private handleSessionTimeout(timeLeft: number): void {
    if (timeLeft <= this.SESSION_WARNING_THRESHOLD) {
      this.showSessionTimeout = true;
      this.sessionTimeoutCounter = timeLeft;
      
      if (timeLeft <= 60) { // Last minute warning
        this.notificationService.showNotification(
          'Your session will expire in less than a minute. Please save your work.',
          NotificationType.WARNING,
          { priority: 5, persistent: true }
        );
      }
    } else {
      this.showSessionTimeout = false;
    }
  }

  /**
   * Refreshes user session
   */
  refreshSession(): void {
    this.authService.refreshToken().subscribe({
      next: () => {
        this.showSessionTimeout = false;
        this.notificationService.showNotification(
          'Session successfully extended',
          NotificationType.SUCCESS
        );
      },
      error: () => {
        this.notificationService.showNotification(
          'Failed to extend session. Please log in again.',
          NotificationType.ERROR
        );
        this.onLogout();
      }
    });
  }
}
import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { AppComponent } from './app.component';
import { LoadingService } from './core/services/loading.service';
import { ErrorService } from './core/services/error.service';
import { WebSocketService, ConnectionStatus } from './core/services/websocket.service';
import { NotificationService } from './core/services/notification.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let loadingService: jasmine.SpyObj<LoadingService>;
  let errorService: jasmine.SpyObj<ErrorService>;
  let webSocketService: jasmine.SpyObj<WebSocketService>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let breakpointObserver: jasmine.SpyObj<BreakpointObserver>;

  // Mock observables
  const loadingSubject = new BehaviorSubject<boolean>(false);
  const wsConnectionSubject = new BehaviorSubject<ConnectionStatus>(ConnectionStatus.DISCONNECTED);

  beforeEach(async () => {
    // Create spy objects for all services
    loadingService = jasmine.createSpyObj('LoadingService', ['show', 'hide', 'reset'], {
      isLoading$: loadingSubject.asObservable()
    });

    errorService = jasmine.createSpyObj('ErrorService', ['handleError', 'showErrorMessage']);

    webSocketService = jasmine.createSpyObj('WebSocketService', 
      ['connect', 'disconnect', 'getConnectionStatus'],
      { isConnected$: wsConnectionSubject.asObservable() }
    );
    webSocketService.getConnectionStatus.and.returnValue(wsConnectionSubject.asObservable());

    notificationService = jasmine.createSpyObj('NotificationService', 
      ['showNotification', 'enableNotifications']
    );

    breakpointObserver = jasmine.createSpyObj('BreakpointObserver', ['observe']);
    breakpointObserver.observe.and.returnValue(of({ matches: false }));

    await TestBed.configureTestingModule({
      declarations: [ AppComponent ],
      providers: [
        { provide: LoadingService, useValue: loadingService },
        { provide: ErrorService, useValue: errorService },
        { provide: WebSocketService, useValue: webSocketService },
        { provide: NotificationService, useValue: notificationService },
        { provide: BreakpointObserver, useValue: breakpointObserver }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    loadingSubject.next(false);
    wsConnectionSubject.next(ConnectionStatus.DISCONNECTED);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.title).toBe('Vessel Call Management System');
  });

  describe('Layout Structure', () => {
    it('should render main layout elements', () => {
      const compiled = fixture.nativeElement;
      expect(compiled.querySelector('mat-toolbar')).toBeTruthy();
      expect(compiled.querySelector('mat-sidenav-container')).toBeTruthy();
      expect(compiled.querySelector('mat-sidenav')).toBeTruthy();
      expect(compiled.querySelector('mat-sidenav-content')).toBeTruthy();
    });

    it('should display app title in toolbar', () => {
      const toolbar = fixture.nativeElement.querySelector('mat-toolbar');
      expect(toolbar.textContent).toContain('Vessel Call Management System');
    });
  });

  describe('Responsive Behavior', () => {
    it('should handle mobile layout', fakeAsync(() => {
      breakpointObserver.observe.and.returnValue(of({ 
        matches: true, 
        breakpoints: { [Breakpoints.HandsetPortrait]: true } 
      }));
      
      component.ngOnInit();
      tick();
      fixture.detectChanges();

      expect(component['isMobile']).toBeTrue();
      expect(fixture.nativeElement.querySelector('mat-sidenav').mode).toBe('over');
    }));

    it('should handle desktop layout', fakeAsync(() => {
      breakpointObserver.observe.and.returnValue(of({ 
        matches: false,
        breakpoints: { [Breakpoints.HandsetPortrait]: false } 
      }));
      
      component.ngOnInit();
      tick();
      fixture.detectChanges();

      expect(component['isMobile']).toBeFalse();
      expect(fixture.nativeElement.querySelector('mat-sidenav').mode).toBe('side');
    }));
  });

  describe('Loading State', () => {
    it('should show loading indicator', fakeAsync(() => {
      loadingSubject.next(true);
      tick(200); // Account for debounce time
      fixture.detectChanges();

      const loadingSpinner = fixture.nativeElement.querySelector('mat-progress-spinner');
      expect(loadingSpinner).toBeTruthy();
      expect(loadingSpinner.getAttribute('mode')).toBe('indeterminate');
    }));

    it('should hide loading indicator', fakeAsync(() => {
      loadingSubject.next(false);
      tick(200);
      fixture.detectChanges();

      const loadingSpinner = fixture.nativeElement.querySelector('mat-progress-spinner');
      expect(loadingSpinner).toBeFalsy();
    }));
  });

  describe('WebSocket Connectivity', () => {
    it('should handle successful connection', fakeAsync(() => {
      webSocketService.connect.and.returnValue(Promise.resolve());
      wsConnectionSubject.next(ConnectionStatus.CONNECTED);
      
      component.ngOnInit();
      tick();
      fixture.detectChanges();

      expect(notificationService.showNotification).toHaveBeenCalledWith(
        'Real-time connection established',
        'SUCCESS',
        jasmine.any(Object)
      );
    }));

    it('should handle connection loss', fakeAsync(() => {
      wsConnectionSubject.next(ConnectionStatus.DISCONNECTED);
      
      component.ngOnInit();
      tick();
      fixture.detectChanges();

      expect(notificationService.showNotification).toHaveBeenCalledWith(
        'Real-time connection lost. Some features may be limited.',
        'WARNING',
        jasmine.any(Object)
      );
    }));

    it('should attempt reconnection on error', fakeAsync(() => {
      webSocketService.connect.and.returnValue(Promise.reject('Connection error'));
      wsConnectionSubject.next(ConnectionStatus.ERROR);
      
      component.ngOnInit();
      tick();
      fixture.detectChanges();

      expect(webSocketService.connect).toHaveBeenCalled();
    }));
  });

  describe('Error Handling', () => {
    it('should display error messages', () => {
      const errorMessage = 'Test error message';
      component['handleError'](new Error(errorMessage));

      expect(errorService.showErrorMessage).toHaveBeenCalledWith(
        errorMessage,
        'Close',
        'error',
        jasmine.any(Number)
      );
    });

    it('should handle API errors', () => {
      const apiError = {
        code: '500',
        message: 'Internal Server Error',
        status: 500,
        timestamp: new Date().toISOString(),
        details: ['Server error occurred'],
        requestId: 'test-123',
        path: '/api/test'
      };

      component['handleApiError'](apiError);
      expect(errorService.handleApiError).toHaveBeenCalledWith(apiError);
    });
  });

  describe('Lifecycle Hooks', () => {
    it('should initialize services on ngOnInit', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(notificationService.enableNotifications).toHaveBeenCalled();
      expect(webSocketService.connect).toHaveBeenCalled();
    }));

    it('should cleanup on ngOnDestroy', () => {
      component.ngOnDestroy();

      expect(webSocketService.disconnect).toHaveBeenCalled();
    });
  });
});
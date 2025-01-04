import { Component, OnInit, OnDestroy } from '@angular/core'; // @angular/core v16.x
import { Router } from '@angular/router'; // @angular/router v16.x
import { Subject, Subscription } from 'rxjs'; // rxjs ^7.8.0
import { takeUntil } from 'rxjs/operators'; // rxjs/operators ^7.8.0
import { trigger, state, style, transition, animate } from '@angular/animations'; // @angular/animations v16.x

import { User, UserRole } from '@core/auth/user.model';
import { AuthService } from '@core/auth/auth.service';

/**
 * Interface defining the structure of navigation items
 */
interface NavigationItem {
  route: string;
  icon: string;
  label: string;
  roles: UserRole[];
  order: number;
  children?: NavigationItem[];
}

/**
 * Component that implements a responsive navigation sidebar with role-based access control
 */
@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
  animations: [
    trigger('sidebarState', [
      state('expanded', style({ width: '240px' })),
      state('collapsed', style({ width: '64px' })),
      transition('expanded <=> collapsed', animate('200ms ease-in-out'))
    ])
  ]
})
export class SidebarComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  isExpanded = true;
  navigationItems: NavigationItem[] = [];
  private destroy$ = new Subject<void>();
  isLoading = true;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to user changes and update navigation
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUser = user;
        if (user) {
          this.updateNavigationItems(user);
        }
        this.isLoading = false;
      });

    // Restore sidebar state from local storage
    const savedState = localStorage.getItem('sidebarExpanded');
    if (savedState !== null) {
      this.isExpanded = JSON.parse(savedState);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Toggles the sidebar expansion state
   */
  toggleSidebar(): void {
    this.isExpanded = !this.isExpanded;
    localStorage.setItem('sidebarExpanded', JSON.stringify(this.isExpanded));
  }

  /**
   * Updates navigation items based on user role and permissions
   */
  private updateNavigationItems(user: User): void {
    const items: NavigationItem[] = [
      // Dashboard - accessible to all authenticated users
      {
        route: '/dashboard',
        icon: 'dashboard',
        label: 'Dashboard',
        roles: [UserRole.PORT_AUTHORITY, UserRole.VESSEL_AGENT, UserRole.SERVICE_PROVIDER, UserRole.SYSTEM_ADMIN],
        order: 1
      },

      // Vessel Calls - accessible to Port Authority and Vessel Agents
      {
        route: '/vessel-calls',
        icon: 'directions_boat',
        label: 'Vessel Calls',
        roles: [UserRole.PORT_AUTHORITY, UserRole.VESSEL_AGENT, UserRole.SYSTEM_ADMIN],
        order: 2,
        children: [
          {
            route: '/vessel-calls/new',
            icon: 'add',
            label: 'New Vessel Call',
            roles: [UserRole.PORT_AUTHORITY, UserRole.VESSEL_AGENT, UserRole.SYSTEM_ADMIN],
            order: 1
          },
          {
            route: '/vessel-calls/list',
            icon: 'list',
            label: 'All Vessel Calls',
            roles: [UserRole.PORT_AUTHORITY, UserRole.VESSEL_AGENT, UserRole.SYSTEM_ADMIN],
            order: 2
          }
        ]
      },

      // Berth Management - accessible to Port Authority only
      {
        route: '/berth-management',
        icon: 'dock',
        label: 'Berth Management',
        roles: [UserRole.PORT_AUTHORITY, UserRole.SYSTEM_ADMIN],
        order: 3,
        children: [
          {
            route: '/berth-management/planning',
            icon: 'event',
            label: 'Planning Board',
            roles: [UserRole.PORT_AUTHORITY, UserRole.SYSTEM_ADMIN],
            order: 1
          },
          {
            route: '/berth-management/conflicts',
            icon: 'warning',
            label: 'Conflict Resolution',
            roles: [UserRole.PORT_AUTHORITY, UserRole.SYSTEM_ADMIN],
            order: 2
          }
        ]
      },

      // Services - accessible to Service Providers and Vessel Agents
      {
        route: '/services',
        icon: 'build',
        label: 'Port Services',
        roles: [UserRole.SERVICE_PROVIDER, UserRole.VESSEL_AGENT, UserRole.SYSTEM_ADMIN],
        order: 4,
        children: [
          {
            route: '/services/book',
            icon: 'add_shopping_cart',
            label: 'Book Service',
            roles: [UserRole.VESSEL_AGENT, UserRole.SYSTEM_ADMIN],
            order: 1
          },
          {
            route: '/services/manage',
            icon: 'settings',
            label: 'Manage Services',
            roles: [UserRole.SERVICE_PROVIDER, UserRole.SYSTEM_ADMIN],
            order: 2
          }
        ]
      },

      // Clearance - accessible to Port Authority only
      {
        route: '/clearance',
        icon: 'gavel',
        label: 'Clearance',
        roles: [UserRole.PORT_AUTHORITY, UserRole.SYSTEM_ADMIN],
        order: 5,
        children: [
          {
            route: '/clearance/pending',
            icon: 'hourglass_empty',
            label: 'Pending Approvals',
            roles: [UserRole.PORT_AUTHORITY, UserRole.SYSTEM_ADMIN],
            order: 1
          },
          {
            route: '/clearance/history',
            icon: 'history',
            label: 'Clearance History',
            roles: [UserRole.PORT_AUTHORITY, UserRole.SYSTEM_ADMIN],
            order: 2
          }
        ]
      },

      // Admin Panel - accessible to System Admin only
      {
        route: '/admin',
        icon: 'admin_panel_settings',
        label: 'Administration',
        roles: [UserRole.SYSTEM_ADMIN],
        order: 6,
        children: [
          {
            route: '/admin/users',
            icon: 'people',
            label: 'User Management',
            roles: [UserRole.SYSTEM_ADMIN],
            order: 1
          },
          {
            route: '/admin/settings',
            icon: 'settings',
            label: 'System Settings',
            roles: [UserRole.SYSTEM_ADMIN],
            order: 2
          }
        ]
      }
    ];

    // Filter items based on user role
    this.navigationItems = items
      .filter(item => this.authService.hasAnyRole(item.roles))
      .map(item => ({
        ...item,
        children: item.children?.filter(child => this.authService.hasAnyRole(child.roles))
      }))
      .sort((a, b) => a.order - b.order);
  }
}
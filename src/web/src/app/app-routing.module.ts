import { NgModule } from '@angular/core'; // @angular/core v16.0.0
import { RouterModule, Routes, PreloadAllModules, UrlSerializer } from '@angular/router'; // @angular/router v16.0.0
import { AuthGuard } from './core/auth/auth.guard';
import { UserRole } from './core/auth/user.model';

/**
 * Main routing configuration for the Vessel Call Management System.
 * Implements secure routes with role-based access control, lazy loading,
 * and comprehensive error handling.
 */
const routes: Routes = [
  // Default redirect to dashboard
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },

  // Authentication routes (publicly accessible)
  {
    path: 'login',
    loadChildren: () => import('./features/auth/auth.module')
      .then(m => m.AuthModule)
  },

  // Core application routes (protected)
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.module')
      .then(m => m.DashboardModule),
    canActivate: [AuthGuard],
    data: {
      roles: [
        UserRole.PORT_AUTHORITY,
        UserRole.VESSEL_AGENT,
        UserRole.SERVICE_PROVIDER,
        UserRole.SYSTEM_ADMIN
      ]
    }
  },
  {
    path: 'vessel-calls',
    loadChildren: () => import('./features/vessel-calls/vessel-calls.module')
      .then(m => m.VesselCallsModule),
    canActivate: [AuthGuard],
    data: {
      roles: [
        UserRole.PORT_AUTHORITY,
        UserRole.VESSEL_AGENT,
        UserRole.SYSTEM_ADMIN
      ]
    }
  },
  {
    path: 'berth-management',
    loadChildren: () => import('./features/berth-management/berth-management.module')
      .then(m => m.BerthManagementModule),
    canActivate: [AuthGuard],
    data: {
      roles: [
        UserRole.PORT_AUTHORITY,
        UserRole.SYSTEM_ADMIN
      ]
    }
  },
  {
    path: 'service-booking',
    loadChildren: () => import('./features/service-booking/service-booking.module')
      .then(m => m.ServiceBookingModule),
    canActivate: [AuthGuard],
    data: {
      roles: [
        UserRole.VESSEL_AGENT,
        UserRole.SERVICE_PROVIDER,
        UserRole.SYSTEM_ADMIN
      ]
    }
  },
  {
    path: 'clearance',
    loadChildren: () => import('./features/clearance/clearance.module')
      .then(m => m.ClearanceModule),
    canActivate: [AuthGuard],
    data: {
      roles: [
        UserRole.PORT_AUTHORITY,
        UserRole.VESSEL_AGENT,
        UserRole.SYSTEM_ADMIN
      ]
    }
  },

  // Error and utility routes
  {
    path: 'unauthorized',
    loadChildren: () => import('./features/error/error.module')
      .then(m => m.ErrorModule),
    data: { errorType: 'unauthorized' }
  },
  {
    path: 'not-found',
    loadChildren: () => import('./features/error/error.module')
      .then(m => m.ErrorModule),
    data: { errorType: 'not-found' }
  },

  // Catch-all route for unmatched paths
  {
    path: '**',
    redirectTo: '/not-found'
  }
];

/**
 * Main routing module that configures application routes with advanced security,
 * lazy loading, and performance optimizations.
 */
@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      // Enable route tracing in development
      enableTracing: false,

      // Use hash-based routing for better compatibility
      useHash: true,

      // Preload all modules for better UX
      preloadingStrategy: PreloadAllModules,

      // Restore scroll position on navigation
      scrollPositionRestoration: 'enabled',

      // Configure route parameter inheritance
      paramsInheritanceStrategy: 'always',

      // Use legacy URL handling for compatibility
      relativeLinkResolution: 'legacy',

      // Custom error handler for malformed URLs
      malformedUriErrorHandler: (
        error: URIError,
        urlSerializer: UrlSerializer,
        url: string
      ) => urlSerializer.parse('/not-found')
    })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {}
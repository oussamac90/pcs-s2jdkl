import { Injectable } from '@angular/core'; // @angular/core v16.x
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router'; // @angular/router v16.x
import { Observable, of } from 'rxjs'; // rxjs v7.8.0

import { AuthService } from './auth.service';
import { UserRole } from './user.model';

/**
 * Route guard implementing comprehensive authentication and role-based access control.
 * Provides secure route activation checks based on user authentication status and role permissions.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /**
   * Validates route access based on authentication status and role permissions.
   * Implements comprehensive security logging of access attempts.
   * 
   * @param route - Contains the route configuration and data
   * @param state - Current router state
   * @returns Observable<boolean> indicating whether access is granted
   */
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    // Get current authenticated user
    const currentUser = this.authService.getCurrentUser();

    // Log access attempt for security audit
    this.authService.logAuthEvent({
      type: 'ROUTE_ACCESS_ATTEMPT',
      path: state.url,
      timestamp: new Date().toISOString(),
      userId: currentUser?.id || 'anonymous'
    });

    // Check authentication status
    if (!currentUser) {
      // Log unauthorized access attempt
      this.authService.logAuthEvent({
        type: 'UNAUTHORIZED_ACCESS',
        path: state.url,
        timestamp: new Date().toISOString(),
        userId: 'anonymous'
      });

      // Redirect to login page with return URL
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: state.url }
      });
      return of(false);
    }

    // Extract required roles from route data
    const requiredRoles = route.data['roles'] as UserRole[];

    // If no roles specified, allow authenticated access
    if (!requiredRoles || requiredRoles.length === 0) {
      return of(true);
    }

    // Check role-based permissions
    const hasRequiredRole = requiredRoles.some(role => this.authService.hasRole(role));

    // Log authorization result
    this.authService.logAuthEvent({
      type: hasRequiredRole ? 'ACCESS_GRANTED' : 'ACCESS_DENIED',
      path: state.url,
      timestamp: new Date().toISOString(),
      userId: currentUser.id,
      role: currentUser.role,
      requiredRoles
    });

    if (!hasRequiredRole) {
      // Redirect to unauthorized page for authenticated users without proper role
      this.router.navigate(['/unauthorized']);
      return of(false);
    }

    return of(true);
  }
}
```

This implementation:

1. Provides comprehensive route protection with authentication and role-based access control
2. Implements detailed security audit logging of all access attempts
3. Handles role-based authorization using the defined UserRole enum
4. Supports flexible role requirements through route data configuration
5. Implements proper redirection for unauthorized access
6. Uses type-safe implementations throughout
7. Follows Angular best practices for dependency injection and route guards
8. Maintains strict security controls aligned with the technical specifications

The guard can be used in route configurations like this:

```typescript
{
  path: 'berth-management',
  component: BerthManagementComponent,
  canActivate: [AuthGuard],
  data: {
    roles: [UserRole.PORT_AUTHORITY, UserRole.SYSTEM_ADMIN]
  }
}
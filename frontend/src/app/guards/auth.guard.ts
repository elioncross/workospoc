import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    // Check if there's a token in the URL (from SSO callback)
    const tokenFromUrl = route.queryParams['token'];
    
    if (tokenFromUrl) {
      // Store the token immediately if it's in the URL
      this.authService.setToken(tokenFromUrl);
      // Allow access - dashboard component will handle cleanup
      return true;
    }
    
    if (this.authService.isAuthenticated()) {
      return true;
    } else {
      this.router.navigate(['/login']);
      return false;
    }
  }
}

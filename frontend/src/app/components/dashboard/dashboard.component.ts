import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;

  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    // Check for token from SSO callback
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      if (token) {
        console.log('SSO token received, authenticating user...');
        try {
          // Store the token and authenticate (AuthGuard may have already done this, but ensure it's done)
          this.authService.setToken(token);
          // Wait a moment for token to be stored and user to be fetched, then navigate
          setTimeout(() => {
            this.router.navigate(['/dashboard'], { replaceUrl: true });
          }, 100);
        } catch (error) {
          console.error('Error storing SSO token:', error);
          this.router.navigate(['/login'], { queryParams: { error: 'token_storage_failed' } });
        }
      } else {
        // No token in URL, check if already authenticated
        if (!this.authService.isAuthenticated()) {
          console.log('No token found and not authenticated, redirecting to login');
          this.router.navigate(['/login']);
        }
      }
    });

    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }
}

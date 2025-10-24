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
        // Store the token and authenticate
        this.authService.setToken(token);
        // Remove token from URL for security
        this.router.navigate(['/dashboard'], { replaceUrl: true });
      }
    });

    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }
}

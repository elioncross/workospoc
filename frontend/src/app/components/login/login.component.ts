import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage = '';
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  ngOnInit() {
    // Check for token in URL (in case user is redirected here with token)
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      if (token) {
        console.log('Token found in login URL, storing and redirecting to dashboard');
        this.authService.setToken(token);
        this.router.navigate(['/dashboard']);
        return;
      }
      
      // Check for error messages
      if (params['error']) {
        this.errorMessage = params['message'] || 'An error occurred during authentication';
        console.error('Login error:', params['error'], this.errorMessage);
      }
    });

    // Check if already authenticated
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';

      const { username, password } = this.loginForm.value;

      this.authService.login(username, password).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.router.navigate(['/dashboard']);
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = 'Invalid username or password';
          console.error('Login error:', error);
        }
      });
    }
  }

  loginWithSSO() {
    // Redirect to WorkOS AuthKit SSO endpoint
    window.location.href = 'http://localhost:8081/api/auth/sso/workos';
  }
}

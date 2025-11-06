export interface User {
  username: string;
  corpId: string;
  role: string;
  authenticated: boolean;
  connectionId?: string;
  idpLogo?: string;
  idpName?: string;
  fullName?: string;
  connectionType?: string;
  organizationId?: string;
}

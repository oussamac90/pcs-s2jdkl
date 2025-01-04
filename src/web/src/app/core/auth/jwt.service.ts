import { Injectable } from '@angular/core'; // @angular/core v16.x
import jwtDecode from 'jwt-decode'; // jwt-decode v3.1.2

@Injectable({
  providedIn: 'root'
})
export class JwtService {
  private readonly TOKEN_KEY = 'vcms_token';
  private readonly REFRESH_TOKEN_KEY = 'vcms_refresh_token';

  constructor() {}

  /**
   * Retrieves the stored JWT token with validation
   * @returns The stored JWT token if valid, null otherwise
   */
  getToken(): string | null {
    try {
      const token = localStorage.getItem(this.TOKEN_KEY);
      if (!token) {
        return null;
      }

      // Validate token format
      if (!this.isValidTokenFormat(token)) {
        this.removeToken();
        return null;
      }

      return token;
    } catch (error) {
      console.error('Error retrieving token:', error);
      return null;
    }
  }

  /**
   * Retrieves the stored refresh token
   * @returns The stored refresh token if exists, null otherwise
   */
  getRefreshToken(): string | null {
    try {
      const refreshToken = localStorage.getItem(this.REFRESH_TOKEN_KEY);
      if (!refreshToken) {
        return null;
      }

      // Validate refresh token format
      if (!this.isValidTokenFormat(refreshToken)) {
        this.removeRefreshToken();
        return null;
      }

      return refreshToken;
    } catch (error) {
      console.error('Error retrieving refresh token:', error);
      return null;
    }
  }

  /**
   * Securely saves JWT token to storage with validation
   * @param token The JWT token to store
   */
  saveToken(token: string): void {
    try {
      if (!token || !this.isValidTokenFormat(token)) {
        throw new Error('Invalid token format');
      }

      localStorage.setItem(this.TOKEN_KEY, token);
    } catch (error) {
      console.error('Error saving token:', error);
      this.removeToken();
    }
  }

  /**
   * Securely saves refresh token to storage
   * @param refreshToken The refresh token to store
   */
  saveRefreshToken(refreshToken: string): void {
    try {
      if (!refreshToken || !this.isValidTokenFormat(refreshToken)) {
        throw new Error('Invalid refresh token format');
      }

      localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    } catch (error) {
      console.error('Error saving refresh token:', error);
      this.removeRefreshToken();
    }
  }

  /**
   * Securely removes JWT token from storage
   */
  removeToken(): void {
    try {
      localStorage.removeItem(this.TOKEN_KEY);
    } catch (error) {
      console.error('Error removing token:', error);
    }
  }

  /**
   * Securely removes refresh token from storage
   */
  removeRefreshToken(): void {
    try {
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    } catch (error) {
      console.error('Error removing refresh token:', error);
    }
  }

  /**
   * Comprehensively validates stored token including expiration and format
   * @returns boolean indicating token validity
   */
  isTokenValid(): boolean {
    try {
      const token = this.getToken();
      if (!token) {
        return false;
      }

      // Validate token format
      if (!this.isValidTokenFormat(token)) {
        return false;
      }

      // Decode and validate token payload
      const decodedToken = this.decodeToken(token);
      if (!decodedToken) {
        return false;
      }

      // Check token expiration
      const currentTime = Math.floor(Date.now() / 1000);
      if (decodedToken.exp && decodedToken.exp < currentTime) {
        this.removeToken();
        return false;
      }

      // Verify required claims
      if (!this.validateTokenClaims(decodedToken)) {
        return false;
      }

      return true;
    } catch (error) {
      console.error('Error validating token:', error);
      return false;
    }
  }

  /**
   * Securely decodes JWT token payload with validation
   * @param token The JWT token to decode
   * @returns Decoded token payload or null if invalid
   */
  decodeToken(token: string): any {
    try {
      if (!this.isValidTokenFormat(token)) {
        return null;
      }

      const decodedToken = jwtDecode(token);
      if (!decodedToken || typeof decodedToken !== 'object') {
        return null;
      }

      return decodedToken;
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  }

  /**
   * Validates JWT token format
   * @param token The token to validate
   * @returns boolean indicating if token format is valid
   */
  private isValidTokenFormat(token: string): boolean {
    try {
      // Check if token is a non-empty string
      if (!token || typeof token !== 'string') {
        return false;
      }

      // Validate JWT format (three parts separated by dots)
      const parts = token.split('.');
      if (parts.length !== 3) {
        return false;
      }

      // Validate each part is base64 encoded
      return parts.every(part => this.isBase64Encoded(part));
    } catch (error) {
      console.error('Error validating token format:', error);
      return false;
    }
  }

  /**
   * Validates required token claims
   * @param decodedToken The decoded token payload
   * @returns boolean indicating if required claims are valid
   */
  private validateTokenClaims(decodedToken: any): boolean {
    try {
      // Check required claims
      const requiredClaims = ['sub', 'iat', 'exp'];
      return requiredClaims.every(claim => claim in decodedToken);
    } catch (error) {
      console.error('Error validating token claims:', error);
      return false;
    }
  }

  /**
   * Validates if a string is base64 encoded
   * @param str The string to validate
   * @returns boolean indicating if string is base64 encoded
   */
  private isBase64Encoded(str: string): boolean {
    try {
      return btoa(atob(str)) === str;
    } catch (error) {
      return false;
    }
  }
}
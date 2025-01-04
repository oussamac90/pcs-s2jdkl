// @angular/core/testing ^16.0.0
import { getTestBed } from '@angular/core/testing';
// @angular/platform-browser-dynamic/testing ^16.0.0
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting
} from '@angular/platform-browser-dynamic/testing';

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting(),
);

// Configure Jasmine test framework (~4.6.0)
declare const jasmine: any;
jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;

// Karma test runner configuration
declare const __karma__: any;
declare const require: any;

// Prevent Karma from running prematurely
__karma__.loaded = function() {};

// Configure test coverage reporting
__karma__.config.coverageReporter = {
  dir: './coverage',
  reporters: [
    { type: 'html', subdir: 'html' },
    { type: 'lcov', subdir: 'lcov' },
    { type: 'text-summary' }
  ],
  fixWebpackSourcePaths: true,
  thresholds: {
    statements: 80,
    lines: 80,
    branches: 80,
    functions: 80
  }
};

// Load all test files
const context = require.context('./', true, /\.spec\.ts$/);
context.keys().map(context);

// Start Karma
__karma__.start();

// Configure browser support
const browserSupport = ['Chrome', 'Firefox'];

// Configure reporting options
const reportingOptions = {
  reporters: ['progress', 'coverage-istanbul'],
  outputDir: './coverage'
};

// Error handler for test failures
const originalError = console.error;
console.error = function(...args: any[]) {
  originalError.apply(console, args);
  // Fail tests on console errors
  if (jasmine && jasmine.currentTest) {
    jasmine.currentTest.fail();
  }
};

// Watch mode configuration
if (__karma__.config.autoWatch) {
  console.log('Running tests in watch mode');
}

// Test environment initialization complete
console.log('Angular testing environment initialized');
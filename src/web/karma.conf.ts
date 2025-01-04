// Karma configuration file for enterprise Angular testing
// External dependencies:
// karma: ~6.4.0
// @types/jasmine: ~4.3.0
// karma-jasmine: ~5.1.0
// karma-chrome-launcher: ~3.2.0
// karma-firefox-launcher: ~2.1.2
// karma-coverage: ~2.2.0
// karma-jasmine-html-reporter: ~2.1.0
// karma-sonarqube-reporter: ~1.4.0

import { Config, ConfigOptions } from 'karma';

export default function(config: Config): void {
  config.set({
    // Base path used for resolving test files and dependencies
    basePath: '',

    // Test frameworks to be used
    frameworks: [
      'jasmine',
      '@angular-devkit/build-angular'
    ],

    // Required plugins for enterprise testing setup
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-firefox-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('karma-sonarqube-reporter'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],

    // Client configuration for Jasmine
    client: {
      clearContext: false, // Leave Jasmine Spec Runner output visible
      jasmine: {
        random: true, // Randomize test execution order
        timeoutInterval: 10000, // Increase timeout for complex tests
        failFast: false, // Continue execution even if a test fails
        verboseDeprecations: true // Show detailed deprecation warnings
      }
    },

    // Coverage reporter configuration
    coverageReporter: {
      dir: 'coverage',
      reporters: [
        { type: 'html', dir: 'coverage/html' },
        { type: 'lcov', dir: 'coverage/lcov' },
        { type: 'cobertura', dir: 'coverage/cobertura' },
        { type: 'text-summary' }
      ],
      check: {
        global: {
          statements: 80,
          branches: 80,
          functions: 80,
          lines: 80
        }
      },
      watermarks: {
        statements: [70, 80],
        branches: [70, 80],
        functions: [70, 80],
        lines: [70, 80]
      }
    },

    // SonarQube reporter configuration
    sonarqubeReporter: {
      basePath: 'src/app',
      filePattern: '**/*spec.ts',
      encoding: 'utf-8',
      outputFolder: 'coverage/sonarqube',
      legacyMode: false,
      reportName: 'karma-report'
    },

    // Test results reporters
    reporters: [
      'progress',
      'kjhtml',
      'coverage',
      'sonarqube'
    ],

    // Web server port
    port: 9876,

    // Enable colors in reporter and logs
    colors: true,

    // Level of logging
    logLevel: config.LOG_INFO,

    // Enable file watching for automatic re-run
    autoWatch: true,

    // Supported browser launchers
    browsers: [
      'Chrome',
      'ChromeHeadless',
      'Firefox'
    ],

    // Custom launcher configurations
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: [
          '--no-sandbox',
          '--disable-gpu',
          '--disable-dev-shm-usage',
          '--disable-software-rasterizer',
          '--disable-extensions'
        ]
      },
      FirefoxHeadlessCI: {
        base: 'Firefox',
        flags: ['-headless']
      }
    },

    // Continuous Integration mode
    singleRun: false,

    // Restart on file changes
    restartOnFileChange: true,

    // Fail on empty test suite
    failOnEmptyTestSuite: true,

    // Timeouts
    captureTimeout: 60000,
    browserDisconnectTimeout: 10000,
    browserNoActivityTimeout: 60000,
    processKillTimeout: 10000,

    // Parallel testing configuration
    concurrency: 4,

    // File patterns from tsconfig.spec.json
    files: [
      { pattern: './src/**/*.spec.ts', watched: true },
      { pattern: './src/**/*.d.ts', watched: false }
    ],

    // Preprocessors for source files
    preprocessors: {
      './src/**/*.ts': ['coverage']
    },

    // Mime types for proper file serving
    mime: {
      'text/x-typescript': ['ts', 'tsx']
    }
  } as ConfigOptions);
}
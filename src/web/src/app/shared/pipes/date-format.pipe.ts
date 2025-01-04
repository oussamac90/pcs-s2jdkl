import { Pipe, PipeTransform } from '@angular/core'; // @angular/core v16.x
import { DatePipe } from '@angular/common'; // @angular/common v16.x

@Pipe({
  name: 'dateFormat',
  pure: true
})
export class DateFormatPipe implements PipeTransform {
  private readonly defaultFormat = 'dd/MM/yyyy HH:mm';
  private readonly maritimeFormats = {
    full: 'dd/MM/yyyy HH:mm',
    date: 'dd/MM/yyyy',
    time: 'HH:mm',
    utc: 'dd/MM/yyyy HH:mm\'Z\'',
    schedule: 'HH:mm dd/MM/yyyy'
  };

  // Regular expression for validating date string formats
  private readonly dateValidationRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?Z?$/;

  constructor(private datePipe: DatePipe) {}

  /**
   * Transforms a date value into a formatted string according to maritime industry standards
   * @param value - The date value to format (Date object or ISO string)
   * @param format - Optional format string or predefined maritime format key
   * @returns Formatted date string or empty string if input is invalid
   */
  transform(value: Date | string | null | undefined, format?: string): string {
    try {
      // Handle null/undefined values
      if (value == null) {
        return '';
      }

      // Convert string dates to Date objects
      let dateValue: Date;
      if (typeof value === 'string') {
        // Validate string format
        if (!this.dateValidationRegex.test(value)) {
          console.warn(`DateFormatPipe: Invalid date string format: ${value}`);
          return '';
        }
        dateValue = new Date(value);
      } else {
        dateValue = value;
      }

      // Validate Date object
      if (isNaN(dateValue.getTime())) {
        console.warn(`DateFormatPipe: Invalid date value: ${value}`);
        return '';
      }

      // Determine format to use
      let formatString = format || this.defaultFormat;
      if (format && format in this.maritimeFormats) {
        formatString = this.maritimeFormats[format as keyof typeof this.maritimeFormats];
      }

      // Apply timezone handling for international ports
      // Note: Using UTC for consistent display across different port locations
      const utcDate = new Date(dateValue.getTime() + dateValue.getTimezoneOffset() * 60000);

      // Format the date using Angular's DatePipe with the determined format
      const formattedDate = this.datePipe.transform(utcDate, formatString, 'UTC');

      // Return formatted date or empty string if formatting failed
      return formattedDate || '';

    } catch (error) {
      console.error('DateFormatPipe: Error formatting date:', error);
      return '';
    }
  }

  /**
   * Validates if a given format string is supported
   * @param format - Format string to validate
   * @returns boolean indicating if format is supported
   * @private
   */
  private isValidFormat(format: string): boolean {
    return format in this.maritimeFormats || /^[dMyHmsz/: ']+$/.test(format);
  }
}
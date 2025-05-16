//
//  Logger.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 5/13/25.
//

import Foundation

enum Logger {
    private static let logger = ApplicationLogger.shared

    static func debug(_ message: String,
                      category: String = "Debug",
                      file: String = #file,
                      function: String = #function,
                      line: Int = #line)
    {
        logger.log(message, level: .debug, category: category, file: file, function: function, line: line)
    }

    static func info(_ message: String,
                     category: String = "Info",
                     file: String = #file,
                     function: String = #function,
                     line: Int = #line)
    {
        logger.log(message, level: .info, category: category, file: file, function: function, line: line)
    }

    static func warning(_ message: String,
                        category: String = "Warning",
                        file: String = #file,
                        function: String = #function,
                        line: Int = #line)
    {
        logger.log(message, level: .warning, category: category, file: file, function: function, line: line)
    }

    static func error(_ message: String,
                      category: String = "Error",
                      file: String = #file,
                      function: String = #function,
                      line: Int = #line)
    {
        logger.log(message, level: .error, category: category, file: file, function: function, line: line)
    }

    static func critical(_ message: String,
                         category: String = "Critical",
                         file: String = #file,
                         function: String = #function,
                         line: Int = #line)
    {
        logger.log(message, level: .critical, category: category, file: file, function: function, line: line)
    }

    static func getLogsForFeedback() -> String {
        logger.getLogsForFeedback()
    }

    static func getLogsForFeedbackJSON() -> [String: Any] {
        logger.getLogsForFeedbackJSON()
    }

    static func clearLogs() {
        logger.clearLogs()
    }
}

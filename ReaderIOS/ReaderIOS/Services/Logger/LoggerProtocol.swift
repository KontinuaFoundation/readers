//
//  LoggerProtocol.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 5/13/25.
//

import Foundation

protocol LoggerProtocol {
    // swiftlint:disable:next function_parameter_count
    func log(_ message: String, level: LogLevel, category: String, file: String, function: String, line: Int)
    func getRecentLogs(count: Int) -> [LogEntry]
    func getLogsForFeedback() -> String
    func clearLogs()
}

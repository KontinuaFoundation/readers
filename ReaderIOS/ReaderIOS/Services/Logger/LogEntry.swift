//
//  LogEntry.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 5/13/25.
//

import Foundation

struct LogEntry: Codable {
    let timestamp: Date
    let level: LogLevel
    let category: String
    let message: String
    let file: String
    let function: String
    let line: Int

    var formattedMessage: String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"

        let filename = URL(fileURLWithPath: file).lastPathComponent

        return "\(dateFormatter.string(from: timestamp)) \(level.emoji) [\(category)] \(filename):\(line) - \(function) - \(message)"
    }

    var emailFormattedMessage: String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "HH:mm:ss.SSS"

        return "\(dateFormatter.string(from: timestamp)) [\(category)] \(message)"
    }
}

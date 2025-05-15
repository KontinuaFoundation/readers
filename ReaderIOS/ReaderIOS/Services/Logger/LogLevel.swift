//
//  LogLevel.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 5/13/25.
//

import OSLog

enum LogLevel: Int, Comparable, Codable {
    case debug = 0
    case info = 1
    case warning = 2
    case error = 3
    case critical = 4

    var osLogType: OSLogType {
        switch self {
        case .debug:
            .debug
        case .info:
            .info
        case .warning:
            .default
        case .error:
            .error
        case .critical:
            .fault
        }
    }

    var emoji: String {
        switch self {
        case .debug:
            "🐛"
        case .info:
            "ℹ️"
        case .warning:
            "⚠️"
        case .error:
            "❌"
        case .critical:
            "🚨"
        }
    }

    static func < (lhs: LogLevel, rhs: LogLevel) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}

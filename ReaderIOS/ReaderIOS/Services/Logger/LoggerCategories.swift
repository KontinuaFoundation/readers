//
//  LoggerCategories.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 5/13/25.
//

import Foundation

extension Logger {
    // Network specific logging
    enum Network {
        static func request(_ url: String, method: String = "GET") {
            Logger.info("Request: \(method) \(url)", category: "Network")
        }

        static func response(_ url: String, statusCode: Int) {
            let message = "Response: \(url) - Status: \(statusCode)"
            if statusCode >= 400 {
                Logger.error(message, category: "Network")
            } else {
                Logger.info(message, category: "Network")
            }
        }

        static func error(_ error: Error, url: String) {
            Logger.error("Network error for \(url): \(error.localizedDescription)", category: "Network")
        }
    }

    // PDF specific logging
    enum PDF {
        static func documentLoaded(_ workbookId: Int) {
            Logger.info("PDF loaded for workbook: \(workbookId)", category: "PDF")
        }

        static func pageChanged(to page: Int, workbook: Int) {
            Logger.debug("Page changed to \(page) for workbook: \(workbook)", category: "PDF")
        }

        static func annotationSaved() {
            Logger.info("Annotations saved", category: "PDF")
        }
    }

    // User action logging
    enum UserAction {
        static func buttonTapped(_ button: String) {
            Logger.debug("Button tapped: \(button)", category: "UserAction")
        }

        static func viewAppeared(_ view: String) {
            Logger.debug("View appeared: \(view)", category: "UserAction")
        }

        static func featureUsed(_ feature: String) {
            Logger.info("Feature used: \(feature)", category: "UserAction")
        }
    }

    // App lifecycle logging
    enum AppLifecycle {
        static func launched() {
            Logger.info("Application launched", category: "AppLifecycle")
        }

        static func willEnterForeground() {
            Logger.info("App will enter foreground", category: "AppLifecycle")
        }

        static func didEnterBackground() {
            Logger.info("App did enter background", category: "AppLifecycle")
        }

        static func memoryWarning() {
            Logger.warning("Memory warning received", category: "AppLifecycle")
        }
    }
}

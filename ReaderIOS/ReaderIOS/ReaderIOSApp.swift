//
//  ReaderIOSApp.swift
//  ReaderIOS
//
//  Created by Devin Hadley on 10/22/24.
//

import SwiftUI

@main
struct ReaderIOSApp: App {
    @StateObject private var initManager = InitializationManager()
    @Environment(\.scenePhase) var scenePhase
    init() {
        // Log app launch
        Logger.AppLifecycle.launched()

        // Log app version
        if let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
           let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String
        {
            Logger.info("App Version: \(version) (\(build))", category: "AppLifecycle")
        }

        // Log device info
        Logger.info(
            "Device: \(UIDevice.current.model) - iOS \(UIDevice.current.systemVersion)",
            category: "AppLifecycle"
        )
    }

    var body: some Scene {
        WindowGroup {
            if initManager.isInitialized {
                SplitView(
                    workbooks: initManager.workbooks,
                    currentCollection: initManager.latestCollection,
                    selectedWorkbookID: initManager.workbookID,
                    currentWorkbook: initManager.workbook
                )
            } else {
                SplashView(initManager: initManager)
            }
        }
    }
}

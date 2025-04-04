//
//  ReaderIOSApp.swift
//  ReaderIOS
//
//  Created by Devin Hadley on 10/22/24.
//

import SwiftUI

@main
struct ReaderIOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    @StateObject private var initManager = InitializationManager()

    var body: some Scene {
        WindowGroup {
            if initManager.isInitialized {
                SplitView(initialWorkbooks: initManager.workbooks,
                          initialWorkbookID: initManager.workbookID,
                          initialPDFDocument: initManager.pdfDocument,
                          initialCollection: initManager.latestCollection)
            } else {
                SplashView(initManager: initManager)
            }
        }
    }
}

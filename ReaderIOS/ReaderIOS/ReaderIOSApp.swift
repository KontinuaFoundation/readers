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

    var body: some Scene {
        WindowGroup {
            if initManager.isInitialized {
                // Pass any initial data to SplitView as needed.
                SplitView(initialWorkbooks: initManager.workbooks,
                          initialPDFDocument: initManager.pdfDocument)
            } else {
                SplashView(initManager: initManager)
            }
        }
    }
}

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
                // TODO:
                // Why is initial data seperate from current in our split view? Wouldnt current just start as initial?
                // - Devin
                SplitView(initialWorkbooks: initManager.workbooks,
                          initialWorkbookID: initManager.workbookID,
                          initialPDFDocument: initManager.pdfDocument)
            } else {
                SplashView(initManager: initManager)
            }
        }
    }
}

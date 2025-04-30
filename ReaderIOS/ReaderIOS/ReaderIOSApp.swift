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
                SplitView(
                          workbooks: initManager.workbooks,
                          currentCollection: initManager.latestCollection,
                          selectedWorkbookID: 0,
                          currentWorkbook: initManager.workbook,
                          pdfDocument: initManager.pdfDocument
                        
                )
            } else {
                SplashView(initManager: initManager)
            }
        }
    }
}

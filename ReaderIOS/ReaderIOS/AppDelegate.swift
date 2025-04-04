//
//  AppDelegate.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 4/3/25.
//

import UIKit

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_: UIApplication,
                     didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool
    {
        if let infoDictionary = Bundle.main.infoDictionary {
            var mutableInfoDictionary = infoDictionary
            mutableInfoDictionary["UILaunchStoryboardName"] = "LaunchScreen"
        }

        // Add any other application launch setup code here

        return true
    }
}

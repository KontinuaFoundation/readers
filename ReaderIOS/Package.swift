//
//  Package.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/1/25.
//


import PackageDescription

let package = Package(
    name: "ReaderIOS",
    platforms: [
        .iOS(.v18)
    ],
    dependencies: [
        
        .package(url: "https://github.com/realm/SwiftLint", from: "0.57.1")
		.package(url: "https://github.com/nicklockwood/SwiftFormat", from: "0.53.3")
    ],
    targets: [
        .target(
            name: "ReaderIOS",
            plugins: {
                #if !XCODE_CLOUD
                [.plugin(name: "SwiftLintBuildToolPlugin", package: "SwiftLint")]
				[.plugin(name: "SwiftFormatBuildToolPlugin", package: "SwiftFormat")]
                #else
                []
                #endif
            }()
        )
    ]
)

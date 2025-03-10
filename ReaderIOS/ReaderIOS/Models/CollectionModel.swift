//
//  CollectionModel.swift
//  ReaderIOS
//
//  Created by Devin Hadley on 3/9/25.
//

import Foundation

struct Collection: Identifiable, Codable {
    let id: Int
    let majorVersion: Int
    let minorVersion: Int
    let localization: String
    let createdAt: String
    let isReleased: Bool
    
    enum CodingKeys: String, CodingKey {
        case id
        case majorVersion = "major_version"
        case minorVersion = "minor_version"
        case localization
        case createdAt = "creation_date"
        case isReleased = "is_released"
        
    }
    
}

struct DetailedCollection: Identifiable, Codable {
    let id: Int
    let majorVersion: Int
    let minorVersion: Int
    let localization: String
    let createdAt: String
    let isReleased: Bool
    let workbooks: [WorkbookPreview]
    
    enum CodingKeys: String, CodingKey {
        case id
        case majorVersion = "major_version"
        case minorVersion = "minor_version"
        case localization
        case createdAt = "creation_date"
        case isReleased = "is_released"
        case workbooks
    }
}

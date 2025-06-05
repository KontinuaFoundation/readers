//
//  FeedbackSubmission.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 5/13/25.
//

import Foundation

struct FeedbackSubmission: Encodable {
    let userEmail: String
    let description: String
    let workbookId: Int?
    let chapterNumber: Int
    let pageNumber: Int
    let majorVersion: Int
    let minorVersion: Int
    let localization: String
    let logs: String?

    enum CodingKeys: String, CodingKey {
        case userEmail = "user_email"
        case description
        case workbookId = "workbook_id"
        case chapterNumber = "chapter_number"
        case pageNumber = "page_number"
        case majorVersion = "major_version"
        case minorVersion = "minor_version"
        case localization
        case logs
    }
}

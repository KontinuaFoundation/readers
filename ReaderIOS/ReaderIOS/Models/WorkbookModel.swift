//
//  WorkbookModel.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//
import Foundation
import PDFKit

struct Chapter: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let chapNum: Int
    let startPage: Int
    let covers: [Cover]
    let requires: [String]?

    enum CodingKeys: String, CodingKey {
        case id, title, covers, requires
        case chapNum = "chap_num"
        case startPage = "start_page"
    }
}

struct Cover: Identifiable, Codable, Hashable {
    let id: String
    let desc: String
    let videos: [Video]?
    let references: [Reference]?

    enum CodingKeys: String, CodingKey {
        case id, desc, videos, references
    }
}

struct Video: Codable, Hashable {
    let link: String
    let title: String

    enum CodingKeys: String, CodingKey {
        case link, title
    }
}

struct Reference: Codable, Hashable {
    let link: String
    let title: String

    enum CodingKeys: String, CodingKey {
        case link, title
    }
}

struct Workbook: Codable, Hashable, Identifiable {
    let id: Int
    let number: Int
    let pdf: String
    let chapters: [Chapter]
    let collection: Int
}

// When we are seeing which workbooks a collection has...
struct WorkbookPreview: Identifiable, Codable {
    let id: Int
    let number: Int
}

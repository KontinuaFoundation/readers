//
//  WorkbookModel.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//
import PDFKit

struct Chapter: Identifiable, Codable {
    let id: String
    let title: String
    let book: String
    let chapNum: Int
    let covers: [Cover]
    let startPage: Int
    let requires: [String]?

    enum CodingKeys: String, CodingKey {
        case id, title, book, covers, requires
        case chapNum = "chap_num"
        case startPage = "start_page"
    }
}

struct Cover: Identifiable, Codable {
    let id: String
    let desc: String
    let videos: [Video]?
    let references: [Reference]?
}

struct Video: Identifiable, Codable {
    var id = UUID()
    let link: String
    let title: String

    enum CodingKeys: String, CodingKey {
        case link, title
    }
}

struct Reference: Identifiable, Codable {
    var id = UUID()
    let link: String
    let title: String

    enum CodingKeys: String, CodingKey {
        case link, title
    }
}

struct Workbook: Codable, Hashable, Identifiable {
    let id: String
    let metaName: String
    let pdfName: String
}

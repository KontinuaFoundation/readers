//
//  MarkupMenuView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//
import SwiftUI

struct MarkupMenu: View {
    // MARK: - Bindings

    @Binding var selectedScribbleTool: String
    @Binding var exitNotSelected: Bool
    @Binding var showClearAlert: Bool
    @Binding var selectedPenColor: Color
    @Binding var selectedHighlighterColor: Color
    @Binding var isPenSubmenuVisible: Bool
    @Binding var textBoxes: [String: [TextBoxData]]

    // MARK: - Observed Objects

    @ObservedObject var annotationManager: AnnotationStorageManager
    @ObservedObject var textManager: TextManager

    // MARK: - Input Data

    let pagePaths: [String: [(path: Path, color: Color)]]
    let highlightPaths: [String: [(path: Path, color: Color)]]

    // MARK: - Body

    var body: some View {
        Menu {
            // Pen Color Submenu
            Menu {
                penColorButton(color: .black, label: "Black")
                penColorButton(color: .green, label: "Green")
                penColorButton(color: .red, label: "Red")
                penColorButton(color: .blue, label: "Blue")
            } label: {
                HStack {
                    Text("Pen")
                    if selectedScribbleTool == "Pen" {
                        Circle()
                            .fill(selectedPenColor)
                            .frame(width: 10, height: 10)
                    }
                }
            }

            // Highlighter Color Submenu
            Menu {
                highlighterColorButton(color: .yellow, label: "Yellow")
                highlighterColorButton(color: .pink, label: "Pink")
                highlighterColorButton(color: .blue, label: "Blue")
                highlighterColorButton(color: .green, label: "Green")
            } label: {
                HStack {
                    Text("Highlight")
                    if selectedScribbleTool == "Highlight" {
                        Circle()
                            .fill(selectedHighlighterColor)
                            .frame(width: 10, height: 10)
                    }
                }
            }

            Button("Erase") {
                selectScribbleTool("Erase")
                exitNotSelected = true
            }
            Button("Text") {
                selectScribbleTool("Text")
                exitNotSelected = true
            }
            Button("Clear Screen") {
                showClearAlert = true
            }
            Button("Exit Markup") {
                selectScribbleTool("")
                exitNotSelected = false
                annotationManager.saveAnnotations(pagePaths: pagePaths, highlightPaths: highlightPaths)
                textManager.saveTextBoxes(textBoxes: textBoxes)
            }
        } label: {
            Text(selectedScribbleTool.isEmpty ? "Markup" : "Markup: \(selectedScribbleTool)")
                .padding(5)
                .foregroundColor(exitNotSelected ? .pink : .blue)
                .cornerRadius(8)
        }
    }

    // MARK: - Private Helpers

    private func penColorButton(color: Color, label: String) -> some View {
        Button {
            selectPenColor(color)
            selectScribbleTool("Pen")
            exitNotSelected = true
            isPenSubmenuVisible = false
        } label: {
            HStack {
                Text(label)
                if selectedPenColor == color {
                    Image(systemName: "checkmark")
                }
            }
        }
    }

    private func highlighterColorButton(color: Color, label: String) -> some View {
        Button {
            selectHighlightColor(color)
            selectScribbleTool("Highlight")
            exitNotSelected = true
            isPenSubmenuVisible = false
        } label: {
            HStack {
                Text(label)
                if selectedHighlighterColor == color {
                    Image(systemName: "checkmark")
                }
            }
        }
    }

    private func selectScribbleTool(_ tool: String) {
        selectedScribbleTool = tool
    }

    private func selectPenColor(_ color: Color) {
        selectedPenColor = color
    }

    private func selectHighlightColor(_ color: Color) {
        selectedHighlighterColor = color
    }
}

//
//  MarkupMenuView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//
import SwiftUI

struct MarkupMenu: View {
    @Binding var selectedScribbleTool: String
    @Binding var exitNotSelected: Bool
    @Binding var showClearAlert: Bool
    @Binding var selectedPenColor: Color
    @Binding var selectedHighlighterColor: Color
    @Binding var isPenSubmenuVisible: Bool
    @ObservedObject var annotationManager: AnnotationStorageManager
    @ObservedObject var textManager: TextManager
    @Binding var textBoxes: [String: [TextBoxData]]

    var pagePaths: [String: [(path: Path, color: Color)]]
    var highlightPaths: [String: [(path: Path, color: Color)]]

    var body: some View {
        Menu {
            // pen submenu
            Menu {
                Button {
                    selectedPenColor = .black
                    selectScribbleTool("Pen")
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Black")
                    if selectedPenColor == .black {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedPenColor = .green
                    selectScribbleTool("Pen")
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Green")
                    if selectedPenColor == .green {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedPenColor = .red
                    selectScribbleTool("Pen")
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Red")
                    if selectedPenColor == .red {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedPenColor = .blue
                    selectScribbleTool("Pen")
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Blue")
                    if selectedPenColor == .blue {
                        Image(systemName: "checkmark")
                    }
                }
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
            Menu {
                Button {
                    selectedHighlighterColor = .yellow
                    selectScribbleTool("Highlight")
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Yellow")
                    if selectedHighlighterColor == .yellow {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedHighlighterColor = .pink
                    selectScribbleTool("Highlight")
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Pink")
                    if selectedHighlighterColor == .pink {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedHighlighterColor = .blue
                    selectScribbleTool("Highlight")
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Blue")
                    if selectedHighlighterColor == .blue {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedHighlighterColor = .green
                    selectScribbleTool("Highlight")
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Green")
                    if selectedHighlighterColor == .green {
                        Image(systemName: "checkmark")
                    }
                }
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
                annotationManager.saveAnnotations(
                    pagePaths: pagePaths,
                    highlightPaths: highlightPaths
                )
                textManager.saveTextBoxes(textBoxes: textBoxes)
            }
        } label: {
            Text(selectedScribbleTool.isEmpty ? "Markup" : "Markup: " + selectedScribbleTool)
                .padding(5)
                .foregroundColor(exitNotSelected ? Color.pink : Color.blue)
                .cornerRadius(8)
        }
    }

    private func selectScribbleTool(_ tool: String) {
        selectedScribbleTool = tool
    }
}

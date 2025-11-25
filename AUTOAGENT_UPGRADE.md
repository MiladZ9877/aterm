# AutoAgent System Upgrade Plan

## Overview
This document outlines the comprehensive upgrade to the AutoAgent system to enable intelligent code generation, learning, and debugging based on text classification and metadata analysis.

## Tasks

### Phase 1: Core Architecture & Text Classifier Integration
- [x] **Task 1.1**: Integrate text classifier for user prompt analysis ✅
  - ✅ Created `PromptAnalyzer` to analyze user prompts
  - ✅ Extract intent, file types, metadata, framework type
  - ✅ Generate prompt patterns for similarity matching
  - ✅ Extract import patterns and event handler patterns

- [x] **Task 1.2**: Implement metadata-based code generation flow ✅
  - ✅ User prompt → PromptAnalyzer → File names + Metadata → Code generation
  - ✅ Store generated code with metadata for learning
  - ✅ Enhanced `AutoAgentProvider` to use prompt analysis

- [x] **Task 1.3**: Implement learning storage system ✅
  - ✅ Extended database schema with prompt patterns, framework types, import patterns, event handlers, code templates
  - ✅ Updated `insertOrUpdateLearnedData` to store all new fields
  - ✅ Enable similarity matching for future prompts

### Phase 2: Learning & Retrieval System
- [x] **Task 2.1**: Implement prompt similarity matching ✅
  - ✅ Added `searchByPromptPattern` method to LearningDatabase
  - ✅ When similar prompt is given, retrieve learned patterns
  - ✅ Use learned metadata and code patterns for generation

- [x] **Task 2.2**: Implement background learning (when inactive) ✅
  - ✅ Already implemented - learns from other providers' interactions
  - ✅ Enhanced to store prompt patterns and metadata patterns
  - ✅ Store successful patterns and metadata

- [x] **Task 2.3**: Implement active learning (when AutoAgent is active) ✅
  - ✅ Uses learned data for code generation
  - ✅ Enhanced to use framework knowledge and prompt patterns
  - ✅ Updates learning based on success/failure

### Phase 3: Debug & Error Analysis
- [ ] **Task 3.1**: Implement error prompt analysis
  - Analyze positive/negative prompts for errors
  - Identify files with errors using learned patterns

- [ ] **Task 3.2**: Implement debug flow using learned data
  - Match error patterns to learned fixes
  - Apply learned debugging strategies

### Phase 4: Framework & Language Knowledge Base
- [x] **Task 4.1**: Create hardcoded framework knowledge base ✅
  - ✅ HTML: Tags, attributes, semantic structure, event handlers
  - ✅ CSS: Selectors, properties, layouts, animations, responsive design
  - ✅ JavaScript: ES6+, DOM manipulation, async/await, modules, event handling
  - ✅ Node.js: Express, file system, streams, npm packages, middleware patterns
  - ✅ Python: Standard library, popular packages, async, decorators, type hints
  - ✅ Java: Spring Boot, collections, streams, annotations, design patterns
  - ✅ Kotlin: Coroutines, extension functions, data classes, DSLs
  - ✅ Model-Route-View: MVC, MVP, MVVM patterns, routing, state management

- [x] **Task 4.2**: Implement DB initialization with framework knowledge ✅
  - ✅ Created `FrameworkKnowledgeBase` with comprehensive knowledge
  - ✅ Populate database on initialization with hardcoded framework patterns
  - ✅ Include correct imports, naming conventions, event handlers
  - ✅ Include architecture patterns and best practices

- [x] **Task 4.3**: Implement metadata-based learning ✅
  - ✅ Learn functional code patterns based on metadata
  - ✅ Learn correct import statements (extractImportPatterns)
  - ✅ Learn event handler naming conventions (extractEventHandlerPatterns)
  - ✅ Learn framework-specific patterns

### Phase 5: Command & Test Integration
- [ ] **Task 5.1**: Integrate offline command system
  - Use existing offline command execution
  - Fallback to hardcoded commands when needed

- [ ] **Task 5.2**: Implement test execution using learned patterns
  - Use learned test patterns
  - Apply framework-specific test commands

### Phase 6: Code Generation Engine
- [x] **Task 6.1**: Implement metadata-driven code generation ✅
  - ✅ Generate code based on metadata patterns
  - ✅ Use learned imports and naming conventions
  - ✅ Apply framework-specific patterns
  - ✅ Enhanced `generateFromLearnedPatterns` to use prompt analysis

- [x] **Task 6.2**: Implement code template system ✅
  - ✅ Generate code templates from learned patterns
  - ✅ Customize templates based on metadata
  - ✅ Store templates in database for reuse

## Implementation Priority

1. **High Priority**: Phase 4 (Framework Knowledge Base) - Foundation for everything
2. **High Priority**: Phase 1 (Core Architecture) - Core functionality
3. **Medium Priority**: Phase 2 (Learning System) - Intelligence layer
4. **Medium Priority**: Phase 3 (Debug System) - Error handling
5. **Low Priority**: Phase 5 & 6 (Integration & Generation) - Polish and optimization

## Technical Details

### Database Schema Extensions
- Add `prompt_pattern` field for storing prompt patterns
- Add `metadata_pattern` field for storing metadata structures
- Add `code_template` field for storing code templates
- Add `framework_type` field for categorizing framework knowledge
- Add `import_patterns` field for storing import patterns
- Add `event_handler_patterns` field for storing event handler patterns

### Text Classifier Integration
- Use existing `ClassificationModelManager` for prompt analysis
- Extract intent, file types, and metadata from prompts
- Generate structured metadata for code generation

### Learning Algorithm
- Similarity matching using fuzzy string matching
- Metadata pattern matching
- Code template retrieval based on similarity
- Success/failure feedback loop

## Notes
- All framework knowledge should be comprehensive and cover common patterns
- Learning should be incremental and non-destructive
- Database initialization should be fast and efficient
- Code generation should prioritize learned patterns over hardcoded ones

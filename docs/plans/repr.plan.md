# Plan: Repr Derivation (Products + Sums) TDD process description

## Goal
Produce a TDD process article for `Repr` derivation (products + sums), based strictly on git history of `Repr.scala` and `ReprSpec.scala`.

## Context
- Test file: `src/test/scala/progmeta/ReprSpec.scala`
- Main file: `src/main/scala/progmeta/Repr.scala`
- Core symbols:
    - `Repr.derived`
    - `productRepr`
    - `sumRepr`
    - tuple-based helper(s) for recursive mapping / evidence pairing in tests

## Scope

### In Scope
- TDD-based process description for Repr derivation of products and sums. 
- git history (local repo) for `ReprSpec.scala` and `Repr.scala`.

### Out of Scope
- Anything outside git history of `ReprSpec.scala` and `Repr.scala`.
- Refactors outside `progmeta/Repr*`
- `eval[Tup <: Tuple]` (to be ignored)

## Constraints
- Keep a test-first approach in the process description.
- Use commit messages and diffs as evidence for:
  - `src/test/scala/progmeta/ReprSpec.scala` and 
  - `src/main/scala/progmeta/Repr.scala`.

## Implementation Plan

1. **Prerequisites**
    - README.md is a true index/TOC and each process description lives in its own .md.
    - Add `docs/articles/repr.art.md`

2. **Collect git history for `ReprSpec.scala` and `Repr.scala`**
   - commit messages
   - commit diffs
      
3. **Analyze**
  - Analyze diffs and commit order to understand how `Repr` and `ReprSpec` evolved.
  - Analyze commit messages to understand the motivation for the current state of `Repr` and `ReprSpec`.

4. **Article generation**
  - Generate an article describing the process as per TDD methodology, using the collected information:
      - Start with the first commit that introduced `ReprSpec` and `Repr`.
      - For each further commit, describe the changes made and how they relate to the contract introduced by the test.
      - Highlight any challenges or decisions made during the process.
  - Follow TDD methodology:
      - Red: describe what we want as a failing test
      - Describe how we make it pass
      - Refactor: describe how we remove duplications (including duplications with the test itself a.k.a hardcoded)

## Open Questions
- What known limitations of the final Repr solution should be documented?
- What assumptions about derived types / sum-product cases should be made explicit?

## Definition of Done
- [ ] Article includes Red/Green/Refactor per major commit group
- [ ] Article is in `docs/articles/repr.art.md`
- [ ] Article describes the TDD process for `Repr` derivation of products and sums, including challenges and decisions made.
- [ ] Description is based on git history and TDD methodology.

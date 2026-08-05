# InteriorPro ERP – Project Protection Policy

## Strict Protection Constraints

### Protected Modules (DO NOT MODIFY UNLESS EXPLICITLY REQUESTED)
- Customer Module
- Company Module
- Master Data
- Quotation Module
- History Module
- PDF Engine
- Calculation Engine
- Snapshot Engine
- Room Database
- Repository Layer
- Domain Models
- Existing Navigation Graph

---

## Pre-Change Verification Protocol
Before editing any file:
1. Identify affected files.
2. Explain why each file must change.
3. Confirm that no protected module will be affected.
4. Perform minimal surgical modifications.

---

## File Modification Rules
- **NEVER** rename packages.
- **NEVER** rename existing classes.
- **NEVER** delete existing files.
- **NEVER** replace working implementations.
- **NEVER** duplicate business logic.
- **NEVER** move folders or refactor unrelated code.
- **NEVER** change Room schema, calculation formulas, PDF rendering, or quotation snapshot logic.

---

## Build & Reporting Rules
- Compile project after changes.
- Verify build success.
- Report modified files only and stop.

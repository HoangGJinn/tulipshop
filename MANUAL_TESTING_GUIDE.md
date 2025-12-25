# Manual Testing Guide - Inventory Management Feature

## Overview
This guide provides step-by-step instructions for manually testing the Inventory Management feature. Follow each test case and check off items as you verify them.

---

## Prerequisites
1. Ensure the application is running: `mvn spring-boot:run`
2. Access the application at: `http://localhost:8080`
3. Log in with an ADMIN account
4. Navigate to: `/admin/inventory`

---

## Test 1: Page Load with Sample Data

### Steps:
1. Navigate to `/admin/inventory`
2. Wait for the page to fully load

### Verify:
- [ ] Page loads without errors (check browser console)
- [ ] Sidebar navigation is visible with "Quản lý kho" highlighted
- [ ] Header shows "Quản lý kho hàng" title
- [ ] Inventory table displays with columns: Product Image, Product Name, SKU, Variants, Price, Physical Stock, Reserved Stock, Available Stock, Actions
- [ ] All product data is populated correctly
- [ ] Product images are displayed
- [ ] Stock numbers are visible and formatted correctly
- [ ] Action buttons (Edit, History, Delete) are visible for each row

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 2: Inline Editing and Real-Time Calculation

### Steps:
1. Locate a product row with some reserved stock
2. Note the current values: Physical Stock, Reserved Stock, Available Stock
3. Click on the Physical Stock value (should become editable)
4. Change the physical stock value (try different scenarios below)

### Scenario A: Valid Increase
- [ ] Click on Physical Stock cell
- [ ] Cell becomes editable with input field
- [ ] Current value is pre-filled
- [ ] Type a higher number (e.g., if current is 100, type 150)
- [ ] Available Stock updates in real-time as you type
- [ ] New Available Stock = New Physical - Current Reserved
- [ ] No warning messages appear
- [ ] Click Save button
- [ ] Success notification appears
- [ ] Values update in the table

### Scenario B: Valid Decrease (but still positive available)
- [ ] Click on Physical Stock cell
- [ ] Type a lower number that still keeps Available Stock positive
- [ ] Available Stock updates in real-time
- [ ] No warning appears
- [ ] Save successfully

### Scenario C: Invalid - Would Create Negative Available Stock
- [ ] Click on Physical Stock cell
- [ ] Type a number lower than Reserved Stock
- [ ] Available Stock shows negative value in RED
- [ ] Warning message appears: "Warning: This would result in negative available stock"
- [ ] Save button is disabled OR shows error when clicked
- [ ] Click Cancel
- [ ] Original value is restored

### Scenario D: Cancel Editing
- [ ] Click on Physical Stock cell
- [ ] Change the value
- [ ] Click Cancel button
- [ ] Original value is restored
- [ ] No API call is made (check Network tab)

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 3: Stock Update with Valid and Invalid Values

### Test 3A: Valid Stock Update
1. Select a product with Physical Stock = 100, Reserved Stock = 20
2. Update Physical Stock to 150
3. Verify:
   - [ ] Success toast notification appears
   - [ ] Physical Stock updates to 150
   - [ ] Available Stock updates to 130 (150 - 20)
   - [ ] Table row refreshes with new values
   - [ ] No errors in console

### Test 3B: Invalid - Negative Available Stock
1. Select a product with Reserved Stock = 30
2. Try to update Physical Stock to 20 (would make Available = -10)
3. Verify:
   - [ ] Error message appears: "Cannot set physical stock to 20. Reserved stock is 30..."
   - [ ] Update is rejected
   - [ ] Original values remain unchanged
   - [ ] Error toast notification appears

### Test 3C: Invalid - Non-numeric Value
1. Click on Physical Stock cell
2. Type letters or special characters
3. Verify:
   - [ ] Input validation prevents non-numeric entry OR
   - [ ] Error message appears when trying to save

### Test 3D: Invalid - Negative Number
1. Click on Physical Stock cell
2. Type a negative number (e.g., -10)
3. Verify:
   - [ ] Input validation prevents negative entry OR
   - [ ] Error message appears when trying to save

### Test 3E: Concurrent Modification
1. Open the same inventory page in two browser tabs
2. In Tab 1: Start editing a product's stock
3. In Tab 2: Update the same product's stock and save
4. In Tab 1: Try to save your changes
5. Verify:
   - [ ] Error message appears about concurrent modification
   - [ ] Tab 1 reloads with the latest value from Tab 2
   - [ ] No data loss occurs

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 4: Search and Filters

### Test 4A: Search by Product Name
1. Locate the search box at the top of the table
2. Type a partial product name (e.g., "Áo")
3. Verify:
   - [ ] Table filters to show only matching products
   - [ ] Result count updates (e.g., "Showing X of Y SKUs")
   - [ ] Search is case-insensitive
   - [ ] Partial matches work
4. Clear the search box
5. Verify:
   - [ ] All products are shown again

### Test 4B: Search by SKU
1. Type a SKU value in the search box
2. Verify:
   - [ ] Table filters to show only matching SKU
   - [ ] Result count updates
3. Clear the search

### Test 4C: Filter by Stock Status
1. Click the "Stock Status" dropdown
2. Select "Low Stock"
3. Verify:
   - [ ] Only products with low stock are shown
   - [ ] Result count updates
4. Select "Out of Stock"
5. Verify:
   - [ ] Only out-of-stock products are shown
6. Select "In Stock"
7. Verify:
   - [ ] Only in-stock products are shown
8. Select "All Status"
9. Verify:
   - [ ] All products are shown again

### Test 4D: Filter by Category
1. Click the "Category" dropdown
2. Select a specific category (e.g., "Áo")
3. Verify:
   - [ ] Only products from that category are shown
   - [ ] Result count updates
4. Select "All Categories"
5. Verify:
   - [ ] All products are shown again

### Test 4E: Combined Filters
1. Type a search term
2. Select a stock status filter
3. Select a category filter
4. Verify:
   - [ ] Results match ALL applied filters (AND logic)
   - [ ] Result count is accurate
   - [ ] Clearing one filter keeps others active
5. Clear all filters
6. Verify:
   - [ ] All products are shown

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 5: Export Downloads Correct File

### Steps:
1. Apply some filters (optional - to test filtered export)
2. Click the "Export to Excel" button
3. Wait for the download to complete

### Verify:
- [ ] Excel file downloads successfully
- [ ] Filename includes timestamp (e.g., `inventory_2024-12-25_14-30-00.xlsx`)
- [ ] File opens in Excel/LibreOffice without errors
- [ ] All visible columns are present in the export
- [ ] Column headers match the table headers
- [ ] All visible rows are included in the export
- [ ] Data values match what's shown in the table
- [ ] Formatting is readable (no truncated text)
- [ ] If filters were applied, only filtered data is exported

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 6: Stock History Displays Correctly

### Steps:
1. Select a product that has been updated before
2. Click the "History" button (clock icon) for that product
3. Wait for the history modal to appear

### Verify:
- [ ] Modal opens with title "Stock History for [Product Name]"
- [ ] History table displays with columns: Date/Time, Previous Qty, New Qty, Change, Admin, Reason
- [ ] Records are ordered by date (most recent first)
- [ ] Timestamps are formatted correctly
- [ ] Previous and New quantities are shown
- [ ] Change amount is calculated correctly (New - Previous)
- [ ] Change amount shows + or - prefix
- [ ] Admin username is displayed
- [ ] Reason is displayed (if provided)
- [ ] Modal has a close button
- [ ] Clicking outside the modal closes it
- [ ] Clicking close button closes the modal

### Test with No History:
1. Find a product that has never been updated
2. Click History button
3. Verify:
   - [ ] Modal opens
   - [ ] Message displays: "No history records found" or similar
   - [ ] No errors occur

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 7: Alerts Show Correct Counts

### Test 7A: Uninitialized SKUs Alert
1. Check if there are any product variants without stock records
2. Look for the yellow/orange alert banner at the top

### Verify:
- [ ] If uninitialized SKUs exist, alert banner is visible
- [ ] Alert shows correct count: "X SKUs need initial stock setup"
- [ ] Alert has an icon (warning/info icon)
- [ ] Alert has a dismiss button (X)
- [ ] Clicking the alert filters the table to show only uninitialized variants
- [ ] Clicking dismiss hides the alert
- [ ] Alert reappears on page refresh if issue persists

### Test 7B: Low Stock Alert
1. Check if there are any products with low stock
2. Look for the red alert banner

### Verify:
- [ ] If low stock items exist, alert banner is visible
- [ ] Alert shows correct count: "X SKUs are running low on stock"
- [ ] Alert has an icon (warning icon)
- [ ] Alert has a dismiss button (X)
- [ ] Clicking the alert filters the table to show only low stock items
- [ ] Clicking dismiss hides the alert
- [ ] Alert reappears on page refresh if issue persists

### Test 7C: No Alerts
1. If no issues exist, verify:
   - [ ] No alert banners are shown
   - [ ] Page looks clean without unnecessary warnings

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 8: Bulk Initialization Works

### Steps:
1. Ensure there are some uninitialized product variants (variants without stock records)
2. Click the "Initialize Stock" or "Create initial stock for new SKUs" button
3. Wait for the bulk initialization modal to appear

### Verify:
- [ ] Modal opens with title "Initialize Stock for New SKUs"
- [ ] List of uninitialized variants is displayed
- [ ] Each variant shows: Product Name, Color, Size
- [ ] Each variant has an input field for initial quantity
- [ ] Input fields accept numeric values only
- [ ] Input fields have default value (e.g., 0 or empty)
- [ ] Modal has "Save" and "Cancel" buttons

### Test Bulk Initialization:
1. Enter quantities for multiple variants (e.g., 50, 100, 75)
2. Leave some variants at 0 or empty
3. Click "Save"

### Verify:
- [ ] Success message appears: "Successfully initialized X SKUs"
- [ ] Modal closes
- [ ] Table refreshes to show newly initialized variants
- [ ] Variants with 0 or empty quantity are skipped
- [ ] Uninitialized alert count decreases
- [ ] New stock records appear in the inventory table
- [ ] Physical Stock matches entered quantities
- [ ] Reserved Stock is 0 for new records
- [ ] Available Stock equals Physical Stock

### Test Cancel:
1. Open bulk initialization modal
2. Enter some quantities
3. Click "Cancel"
4. Verify:
   - [ ] Modal closes
   - [ ] No changes are made
   - [ ] No API call is made (check Network tab)

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 9: Dark Mode Toggle

### Steps:
1. Locate the dark mode toggle button (usually in header or sidebar)
2. Note the current theme (light or dark)
3. Click the toggle button

### Verify Light to Dark:
- [ ] Page transitions to dark mode smoothly
- [ ] Background changes to dark color
- [ ] Text changes to light color for readability
- [ ] Table rows have appropriate dark background
- [ ] Hover states work correctly in dark mode
- [ ] Buttons and inputs have dark theme styling
- [ ] Icons are visible in dark mode
- [ ] Status badges (Low Stock, Out of Stock) are readable
- [ ] Modals have dark theme styling
- [ ] No white flashes or jarring transitions

### Verify Dark to Light:
- [ ] Click toggle again
- [ ] Page transitions back to light mode smoothly
- [ ] All elements return to light theme styling
- [ ] Everything remains readable and functional

### Verify Persistence:
- [ ] Refresh the page
- [ ] Theme preference is remembered
- [ ] Page loads in the last selected theme

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Test 10: Responsive Design on Mobile

### Testing Methods:
- Use browser DevTools responsive mode (F12 → Toggle device toolbar)
- Test on actual mobile device if available
- Test multiple screen sizes: 320px, 375px, 768px, 1024px

### Test 10A: Mobile View (320px - 767px)

#### Navigation:
- [ ] Sidebar collapses to hamburger menu
- [ ] Hamburger menu icon is visible
- [ ] Clicking hamburger opens sidebar
- [ ] Sidebar overlays content (doesn't push it)
- [ ] Clicking outside sidebar closes it

#### Header:
- [ ] Title is visible and not truncated
- [ ] Action buttons stack or resize appropriately
- [ ] Export button is accessible

#### Filters and Search:
- [ ] Search box is full width or appropriately sized
- [ ] Filter dropdowns are accessible
- [ ] Dropdowns don't overflow screen

#### Table:
- [ ] Table is scrollable horizontally if needed
- [ ] Important columns (Product Name, Stock) are visible
- [ ] Action buttons are accessible
- [ ] Text is readable (not too small)
- [ ] Touch targets are large enough (min 44x44px)

#### Modals:
- [ ] Modals fit within screen width
- [ ] Modal content is scrollable if needed
- [ ] Close button is accessible
- [ ] Form inputs are usable on mobile

#### Inline Editing:
- [ ] Clicking stock cell opens input
- [ ] Input is large enough for touch
- [ ] Save/Cancel buttons are accessible
- [ ] Keyboard appears for numeric input

### Test 10B: Tablet View (768px - 1023px)
- [ ] Layout adapts appropriately
- [ ] Sidebar may be visible or collapsible
- [ ] Table shows more columns
- [ ] All functionality remains accessible

### Test 10C: Desktop View (1024px+)
- [ ] Full layout is displayed
- [ ] Sidebar is always visible
- [ ] All columns are visible
- [ ] Optimal spacing and readability

### Test 10D: Orientation Changes
1. Test in portrait mode
2. Rotate to landscape mode
3. Verify:
   - [ ] Layout adapts smoothly
   - [ ] No content is cut off
   - [ ] Functionality remains intact

**Status:** ⬜ Pass / ⬜ Fail

**Notes:**
```
[Record any issues or observations here]
```

---

## Additional Tests

### Performance:
- [ ] Page loads in under 3 seconds
- [ ] Table renders smoothly with 100+ items
- [ ] Filtering is instant (no lag)
- [ ] Inline editing is responsive

### Accessibility:
- [ ] Tab navigation works through all interactive elements
- [ ] Focus indicators are visible
- [ ] Screen reader can read table content
- [ ] Color contrast meets WCAG standards

### Error Handling:
- [ ] Network errors show user-friendly messages
- [ ] Failed API calls don't break the UI
- [ ] Loading states are shown during operations

---

## Summary

### Test Results:
- Total Tests: 10
- Passed: ___
- Failed: ___
- Blocked: ___

### Critical Issues Found:
```
[List any critical issues that prevent core functionality]
```

### Minor Issues Found:
```
[List any minor issues or improvements needed]
```

### Overall Assessment:
⬜ Ready for Production
⬜ Needs Minor Fixes
⬜ Needs Major Fixes

---

## Sign-off

**Tester Name:** _______________
**Date:** _______________
**Signature:** _______________

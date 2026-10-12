# Product Requirements: Agentic Order Service

## Purpose

The Agentic Order Service is a small order management application. It supports managing products and customers and creating, storing, and retrieving orders. Prices and discounts are calculated exclusively on the server.

## Functional Requirements

| ID | Requirement |
| --- | --- |
| FR-001 | Create, retrieve individually, update, list, and deactivate products. Each product has an ID, name, gross price in EUR, and active status. |
| FR-002 | Create, retrieve individually, update, list, and delete customers. Each customer has an ID, name, and email address. Deactivate customers referenced by existing orders instead of physically deleting them. |
| FR-003 | Create and store an order for an existing active customer with one or more line items referencing eligible products and specifying quantities. Consolidate duplicate product references. |
| FR-004 | Calculate and preserve product prices, line item amounts, subtotal, discount, and payable total on the server. |
| FR-005 | Retrieve stored orders individually and list them. An individually retrieved order includes its customer reference, line items, original product prices, and calculated amounts. |

## Business Rules

| ID | Rule |
| --- | --- |
| BR-001 | Product prices must be non-negative. All prices are gross amounts in EUR. |
| BR-002 | Deactivated products cannot be included in new orders. Products referenced by existing orders must not be physically deleted. |
| BR-003 | Customers referenced by existing orders must be deactivated instead of physically deleted. Deactivated customers cannot place new orders. |
| BR-004 | Every order belongs to an existing active customer and contains at least one line item. |
| BR-005 | Quantities must be positive integers. Duplicate references to the same product within an order are consolidated into one line item by summing their quantities. |
| BR-006 | Server-side product prices exclusively determine order amounts. Caller-supplied prices and calculated amounts must not override them. |
| BR-007 | Each line item amount equals its product price multiplied by its quantity. The subtotal equals the sum of line item amounts. |
| BR-008 | A subtotal of at least EUR 100.00 qualifies for a 10% discount on the subtotal. Below that threshold, the discount is zero. |
| BR-009 | Monetary calculations use two decimal places. Discounts are rounded to two decimal places using HALF_UP. The payable total equals the subtotal minus the rounded discount. |
| BR-010 | Stored orders preserve their original product prices and calculated amounts, including when products or customers are subsequently changed, deactivated, or deleted where permitted. |

## Acceptance Criteria

| ID | Related requirements / rules | Testable criteria |
| --- | --- | --- |
| AC-001 | FR-001, BR-001 | A product can be created with its required fields, retrieved by ID, listed, and updated. A price of EUR 0.00 is accepted; a negative price is rejected. |
| AC-002 | FR-001, BR-002 | Deactivating a product changes its active status. A new order referencing it is rejected. A product referenced by an existing order remains stored. |
| AC-003 | FR-002 | A customer can be created with an ID, name, and email address, retrieved by ID, listed, and updated. A customer with no existing orders can be physically deleted and is no longer retrievable. |
| AC-004 | FR-002, BR-003 | Deleting a customer referenced by an existing order deactivates that customer and preserves the customer record. A new order for that customer is rejected. |
| AC-005 | FR-003, BR-002–BR-005 | An order for an existing active customer with active products and positive integer quantities is accepted. An unknown or deactivated customer, an unknown or deactivated product, an empty order, or a zero, negative, or fractional quantity causes rejection. |
| AC-006 | FR-003, BR-005 | Two line items referencing the same product with quantities 2 and 3 result in one stored line item with quantity 5. |
| AC-007 | FR-004, BR-006–BR-007 | For a server-side product price of EUR 25.00 and quantity 2, the line item amount is EUR 50.00 regardless of caller-supplied price or total values. |
| AC-008 | FR-004, BR-008–BR-009 | The calculation examples below produce the stated discount and payable total, including the threshold and HALF_UP rounding cases. |
| AC-009 | FR-004–FR-005, BR-010 | After an order is stored, changing a product price does not change its original product prices or calculated amounts when retrieved. Product or customer changes and deactivation do not prevent retrieval of the stored order. |
| AC-010 | FR-005 | A stored order can be retrieved individually with its line items and calculated amounts and appears in the order list. |

### Calculation Examples

| Subtotal | Discount | Payable total |
| --- | --- | --- |
| EUR 90.00 | EUR 0.00 | EUR 90.00 |
| EUR 99.99 | EUR 0.00 | EUR 99.99 |
| EUR 100.00 | EUR 10.00 | EUR 90.00 |
| EUR 100.05 | EUR 10.01 | EUR 90.04 |
| EUR 150.00 | EUR 15.00 | EUR 135.00 |

## MVP Scope

The MVP includes:

- Creating, retrieving, updating, listing, and deactivating products.
- Creating, retrieving, updating, listing, and deleting customers, with deactivation when existing orders reference them.
- Creating and storing orders with validated customers, products, and quantities, including duplicate consolidation.
- Server-side calculation of gross EUR amounts and discounts, preserving the original order prices and amounts.
- Retrieving individual orders and listing orders.

Updating and deleting orders are outside the MVP scope. No additional features, architecture, or technical implementation decisions are defined here.

## Open Business Questions

- Which validation rules apply to product names, customer names, and email addresses, and must customer email addresses be unique?
- Can deactivated products or customers be reactivated?
- Should lists include deactivated products and customers?
- Must tax rates or tax breakdowns be recorded or displayed in addition to gross amounts?
- If a product price is supplied with more than two decimal places, should it be rejected or rounded? If rounded, which rounding rule applies?
- Should existing orders preserve original product names and customer details as well as prices and calculated amounts, or reflect later changes to those details?

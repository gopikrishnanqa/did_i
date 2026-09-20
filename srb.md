## **App Working Name: Did I Remember?**

### **Tagline: Your memory for everyday life**

---

## 1. Product Overview

**Did I Remember?** is a simple personal memory assistant that helps users remember two types of everyday things:

1. **Things they need to remember before doing or leaving somewhere**
  - Phone
  - Wallet
  - Keys
  - Charger
  - Lunch
  - Documents
2. **Things they want to remember they have already completed**
  - Locked the door
  - Fed the cat
  - Took vitamins
  - Sent an email
  - Turned off the lights

The core purpose is to solve two natural everyday questions:

> **"What might I forget?"**

and

> **"Did I already do that?"**

The app must remain lightweight and simple. It should **not feel like a complex productivity, to-do list, habit tracker, or project-management application**.

---



# 2. Core Product Principle

The entire application should be built around the concept of acting as an **external memory for small everyday actions and items**.

### The product should NOT become:

- A complex to-do app
- A project management tool
- A habit tracker
- A calendar application
- A notes application
- A complicated reminder system



### The product SHOULD feel like:

> **"I don't need to remember every small thing. I can quickly check or record it here."**

---



# 3. Target Users

The primary target users are people who frequently:

- Forget items when leaving home
- Wonder whether they completed an action
- Need simple routines for everyday situations
- Want to remember small actions without maintaining a complicated task list

Example situations:

- "Did I lock the door?"
- "Did I take my wallet?"
- "Did I feed the cat?"
- "Did I take my vitamins?"
- "Did I turn off the lights?"
- "What do I need before going to work?"
- "Did I already send that?"

---



# 4. Core Application Structure

The application will have **two primary actions**.

## A. Before I Go

Helps users remember items and actions before leaving or starting a routine.

## B. Did I Do It?

Helps users quickly record completed actions and later check whether they already completed them.

These two features must feel connected under one product concept rather than appearing as two separate applications.

---



# 5. Home Screen

The home screen must be extremely minimal.

## Header

A simple greeting based on time.

Examples:

> Good morning 👋

or

> Good evening 👋

---



## Primary Action 1



### 🚪 Before I Go

Subtitle:

> Don't forget important things.

Show a small preview of the user's most relevant routine.

Example:

**Phone · Keys · Wallet**

Primary button:

> **Start Check**

---



## Primary Action 2



### ✓ Did I Do It?

Subtitle:

> Remember what you've already done.

Show recently tracked actions.

Example:

- Locked door — 8:32 AM
- Fed cat — 7:42 AM
- Took vitamins — Yesterday

Primary button:

> **Mark Something**

---



## Recent Activity

A small chronological list showing recently completed actions.

Example:

✓ Locked the door  
Today · 8:32 AM

✓ Fed the cat  
Today · 7:42 AM

✓ Took vitamins  
Yesterday · 9:15 PM

The home screen must not contain excessive buttons, categories, statistics, charts, or navigation.

---



# 6. Feature: Before I Go



## Purpose

This feature helps users quickly check important items and actions before leaving or beginning a specific situation.

The user selects a routine and checks items.

---



## Example Routine



### 🚪 Leaving Home

**Remember to take:**

☐ Phone  
☐ Wallet  
☐ Keys  
☐ Charger

**Confirm before leaving:**

☐ Locked the door  
☐ Turned off lights

---



## Completion

Once all necessary items are checked, the user can press:

# **I'm Ready**

The routine is saved as completed with:

- Date
- Time
- Routine name

Example:

> Leaving Home completed  
> Today at 8:32 AM

---



# 7. Routine Types

Users can create routines.

Initial suggested routines may include:

### 🚪 Leaving Home



### 💼 Going to Work



### 🛒 Going Shopping



### ✈️ Travelling



### 🌙 Before Sleeping



### ➕ Custom Routine

Users must be able to:

- Rename routines
- Add items
- Remove items
- Reorder items
- Edit items

The application should not force users to use predefined routines.

---



# 8. Feature: Did I Do It?



## Purpose

Users can quickly record an action they have completed.

Examples:

- Fed the cat
- Locked the door
- Took vitamins
- Sent the email
- Watered the plants

---



## Main Action

The screen should prominently show:

# ✓ I DID IT

The interaction must require minimal effort.

---



## Example Flow

User opens **Did I Do It?**

They see frequently used actions:

🐱 Fed the cat

🔒 Locked the door

💊 Took vitamins

💡 Turned off lights

User taps:

> Locked the door

The application immediately saves:

> ✓ Locked the door  
> Today at 8:32 AM

No unnecessary confirmation screens should be required.

---



# 9. Custom Action Creation

Users must be able to create custom actions.

Example:

> What did you do?

User enters:

**Fed the fish**

The app saves:

✓ Fed the fish  
Today at 7:30 AM

The newly created action should automatically become available for quick reuse.

---



# 10. Checking Whether Something Was Done

This is one of the application's most important features.

Users should be able to search for an action.

Example:

> Did I feed the cat?

The application should display:

# ✓ Yes

**Fed the cat**

Today at **7:42 AM**

---

If there is no completion record for the relevant period:

# Not recorded

> You haven't marked this as completed today.

---



## Search

The user should be able to search using keywords.

Example:

Search:

> cat

Results:

- Fed the cat — Today 7:42 AM
- Fed the cat — Yesterday 7:38 AM

---



# 11. Connection Between Both Features

The two features should share information where appropriate.

Example routine:

## Leaving Home

Items:

☐ Phone  
☐ Wallet  
☐ Keys

Confirmation actions:

☐ Locked the door  
☐ Turned off lights

When the user checks:

> Locked the door

The app should save that action as a completion record.

Therefore, later the user can search:

> Locked the door

And see:

✓ Completed today at 8:32 AM

This connection is important because it makes the app feel like one unified product.

---



# 12. Quick Actions

The application should prioritize speed.

Frequently used actions should be easily accessible.

Examples:

### Quick Mark

✓ Locked door

✓ Took vitamins

✓ Fed cat

✓ Turned off lights

The user should be able to mark an action with a single tap.

---



# 13. History

The app should maintain a simple activity history.

Example:

## Today

✓ Locked door — 8:32 AM

✓ Fed cat — 7:42 AM

✓ Took vitamins — 7:10 AM

---



## Yesterday

✓ Locked door — 8:25 AM

✓ Fed cat — 7:35 AM

The history should remain simple and chronological.

No complex productivity analytics are required in the MVP.

---



# 14. Edit / Undo

Because users may accidentally mark something as completed, every recent completion should support:

- Undo
- Delete
- Edit time

Example:

User accidentally taps:

> Fed the cat

They should immediately be able to:

> Undo

This is important for maintaining trust in the app's records.

---



# 15. MVP Scope



## Phase 1 — Initial Release

The first version should contain only the essential features.

### Home

- Before I Go
- Did I Do It?
- Recent activity



### Before I Go

- Create routine
- Add items
- Check items
- Complete routine



### Did I Do It?

- Create actions
- Quick mark actions
- Automatic timestamp
- Frequently used actions



### History

- View today's actions
- View previous actions
- Search actions



### Basic Settings

- Manage routines
- Manage actions
- Basic notification settings

---



# 16. Future Phase Features

These should **not be implemented in the initial MVP unless necessary**.

## Phase 2



### Voice Logging

User says:

> "I fed the cat."

The application records:

✓ Fed the cat  
Today at 7:42 AM

---



### Widgets

Possible widgets:

#### Quick Mark Widget

✓ Lock Door

✓ Feed Cat

✓ Take Vitamins

#### Before I Go Widget

🚪 Start Leaving Home Check

---



### Reminders

Example:

> Did you remember to take your lunch?

or:

> You usually lock the door around this time.

---



# 17. Advanced Future Concept

The long-term product can evolve into a **personal external memory assistant**.

The user should eventually be able to ask:

> Did I take my vitamins?

> When did I last feed the cat?

> What do I usually take when travelling?

> What did I complete before leaving home?

The application can answer using the user's own recorded history.

However, AI functionality should not be forced into the MVP.

The basic manual experience must be useful without AI.

---



# 18. UX Principles

The application must follow these principles:

## 1. One-Tap Actions

Frequently performed actions should require one tap.

---



## 2. Minimal Choices

Do not show too many options simultaneously.

---



## 3. No Productivity Dashboard

Avoid:

- Productivity scores
- Completion percentages
- Complex charts
- Daily streaks in the MVP
- Gamification

---



## 4. Human Language

Use natural wording.

Prefer:

> **Did I do it?**

Instead of:

> Activity Log

Prefer:

> **I'm leaving**

Instead of:

> Execute Routine

Prefer:

> **I did it**

Instead of:

> Create Completion Record

---



# 19. Suggested Navigation

Keep navigation minimal.

### Bottom Navigation

**Home**

**History**

**Settings**

The two core actions should be available directly from the Home screen.

Avoid creating separate bottom navigation tabs for:

- Routines
- Actions
- Checklists
- Reminders
- Categories

These can be managed through Settings or contextual screens.

---



# 20. Design Direction

The design should feel:

- Minimal
- Calm
- Friendly
- Fast
- Personal

The app should avoid looking like:

- A corporate productivity app
- A complex dashboard
- A traditional checklist application



### Visual approach

- Large readable text
- Generous spacing
- Minimal cards
- Clear primary actions
- Soft, simple icons
- Very limited information per screen

The most important screen should make the user immediately understand:

# 🤔 What might I forget?

and

# 🤔 Did I already do it?

---



# 21. Success Criteria

The MVP can be considered successful if a user can:

### Within 5 seconds

Start a "Before I Go" checklist.

### Within 2 seconds

Mark a common action as completed.

### Within 5 seconds

Check whether they completed an action today.

The application should reduce mental load rather than create more things for the user to manage.

---



# 22. Final Product Definition



## **Did I Remember?**

A simple personal memory assistant that helps people:

### Before:

> **Remember what they need to take or check.**



### After:

> **Remember what they have already done.**



### Core Promise:



# **Don't remember everything. Just live.**

---

I recommend this as the final product direction because the two features genuinely belong together. The key is to **keep the MVP extremely focused**. If we add reminders, habits, AI, analytics, categories, and automation too early, it will quickly become another complicated productivity app.

what would be the name memory assitant or suggest som best name with ASO

Yes. For this merged app, **“Memory Assistant” is understandable but too generic** for ASO. It will be difficult to stand out because many apps already use *memory*, *assistant*, *reminder*, etc.

Your app's strongest concept is:

> **“Did I forget something before leaving?” + “Did I already do something?”**

So the name should ideally communicate **remembering, checking, and confirmation**.

## My strongest name recommendations



### 🥇 **Did I?**

**Best for branding and uniqueness**

Examples:

- Did I lock the door?
- Did I take my wallet?
- Did I feed the cat?
- Did I take my medicine?

The entire app concept naturally fits the name.

**Tagline:**

> Remember what you did. Check what you forgot.

**ASO subtitle:**

> Checklist & Daily Activity Log

I particularly like this because it is simple, memorable, and creates curiosity.
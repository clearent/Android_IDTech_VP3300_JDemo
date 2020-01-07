![Screenshot](docs/clearent_logo.jpg)

# Android IDTech VP3300 Java Demo :phone: :credit_card:

:new: This demo shows a common way to interact with the clearent idtech framework without all of the framework capabilities our other demo shows.

This demo uses our new contactless release that's in beta. We have not released it yet (soon).

The demo is broken down into two fragments - settings and payment. The settings fragment allows you change settings used when you run a payment.
It also let's you search for a bluetooth device, select it, and save it for future use. There is also an option to configure the reader. Configuration does
take time so it's recommended to keep this ability separate from the payment flow. Settings gives you an option to toggle between sandbox and production environments.
The values are maintained in the Constants class. Both environments default to sandbox.

The Payment fragment let's you run a payment. If you configured the app to use a bluetooth device you will be asked to press the button on the reader from the pop up. The messages
that come back from the framework will display in the pop up. When you see a message to tap, insert or swipe go ahead and use a card with the reader.
When the card is converted to a transaction token (JWT) the demo will run a sale for the amount you provided. The transaction will run against the environment you selected. The end result is a success message.

The demo has the apikey hard coded in it. This is not recommended in production since it's a secret key that only you and Clearent should know about.

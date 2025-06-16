from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_session import Session
from dotenv import load_dotenv
import os
import smtplib
from email.mime.text import MIMEText

load_dotenv()

def create_app():
    app = Flask(__name__)
    app.config["SESSION_TYPE"] = "filesystem"
    app.config["SECRET"] = os.getenv("SECRET")

    CORS(app, resources={
        r"/mail": {"origins": "*"},
        r"/": {"origins": "*"}
    })
    Session(app)

    @app.get("/")
    def home():
        return jsonify({"Message": "Hi"}), 200
    
    @app.post("/mail")
    def mail():
        request_json = request.json
        recipients = ", ".join(request_json["contacts"])
        print(recipients)
        maps_link = f"https://www.google.com/maps?q={request_json['lat']},{request_json['long']}"
        msg = MIMEText(f"Registered User is in danger!! User at Lat: {request_json['lat']}, Long: {request_json['long']}\nView location on Google Maps: {maps_link}")
        msg["Subject"] = "SOS Call from User"
        msg["From"] = os.getenv("EMAIL_USER")
        msg["To"] = recipients

        try:
            with smtplib.SMTP("smtp.gmail.com", 587) as server:
                server.starttls()
                server.login(os.getenv("EMAIL_USER"), os.getenv("EMAIL_APP_PASSWORD"))
                server.send_message(msg)
            return jsonify({"message": "Email sent successfully"}), 200
        except Exception as e:
            return jsonify({"error": str(e)}), 500

    return app
if __name__ == "__main__":
    app = create_app()
    app.run(host="0.0.0.0", debug=True, port=5000)
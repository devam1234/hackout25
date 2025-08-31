from flask import Flask, request, jsonify, send_from_directory
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash
from flask_cors import CORS
import os
from werkzeug.utils import secure_filename

# --- Flask app setup ---
app = Flask(__name__)
CORS(app)

# --- Database setup ---
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///companies.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

# --- File upload settings ---
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), "uploads")
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER
app.config["MAX_CONTENT_LENGTH"] = 16 * 1024 * 1024  # 16 MB max
ALLOWED_EXTENSIONS = {"pdf", "png", "jpg", "jpeg"}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# --- Company Model ---
class Company(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    company_name = db.Column(db.String(150), nullable=False)
    email = db.Column(db.String(150), unique=True, nullable=False)
    gst_number = db.Column(db.String(100), unique=True, nullable=False)
    password = db.Column(db.String(200), nullable=False)

with app.app_context():
    db.create_all()

# --- Registration API ---
@app.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    company_name = data.get('company_name')
    email = data.get('email')
    gst_number = data.get('gst_number')
    password = data.get('password')

    if not all([company_name, email, gst_number, password]):
        return jsonify({"error": "Missing fields"}), 400

    if Company.query.filter((Company.email == email) | (Company.gst_number == gst_number)).first():
        return jsonify({"error": "Company already registered!"}), 400

    hashed_password = generate_password_hash(password)
    new_company = Company(
        company_name=company_name,
        email=email,
        gst_number=gst_number,
        password=hashed_password
    )
    db.session.add(new_company)
    db.session.commit()
    return jsonify({"message": "Company registered successfully!"}), 201

# --- Login API ---
@app.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    email = data.get('email')
    password = data.get('password')

    if not all([email, password]):
        return jsonify({"error": "Missing fields"}), 400

    company = Company.query.filter_by(email=email).first()
    if not company:
        return jsonify({"error": "Please register first!"}), 400

    if not check_password_hash(company.password, password):
        return jsonify({"error": "Invalid credentials!"}), 401

    return jsonify({"message": f"Welcome, {company.company_name}!"}), 200

# --- File Upload API ---
@app.route('/upload', methods=['POST'])
def upload_file():
    if "document" not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files["document"]

    if file.filename == "":
        return jsonify({"error": "No selected file"}), 400

    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        filepath = os.path.join(app.config["UPLOAD_FOLDER"], filename)
        file.save(filepath)
        return jsonify({"message": f"File uploaded successfully: {filename}"}), 200

    return jsonify({"error": "Invalid file type"}), 400

# --- List Uploaded Files API ---
@app.route('/files', methods=['GET'])
def list_files():
    files = os.listdir(app.config["UPLOAD_FOLDER"])
    return jsonify({"uploaded_files": files})

# --- Download File API ---
@app.route('/download/<filename>', methods=['GET'])
def get_file(filename):
    filepath = os.path.join(app.config["UPLOAD_FOLDER"], filename)
    if not os.path.exists(filepath):
        return jsonify({"error": "File not found"}), 404
    return send_from_directory(app.config["UPLOAD_FOLDER"], filename, as_attachment=True)

# --- Run app ---
if __name__ == '__main__':
    app.run(debug=True)
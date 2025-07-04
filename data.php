<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Create data directory if it doesn't exist
$dataDir = __DIR__ . '/data';
if (!is_dir($dataDir)) {
    mkdir($dataDir, 0755, true);
}

$credentialsFile = $dataDir . '/credentials.json';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get the raw POST data
    $input = file_get_contents('php://input');
    
    if (empty($input)) {
        http_response_code(400);
        echo json_encode(['error' => 'No data received']);
        exit();
    }
    
    // Decode the JSON data
    $data = json_decode($input, true);
    
    if (json_last_error() !== JSON_ERROR_NONE) {
        http_response_code(400);
        echo json_encode(['error' => 'Invalid JSON data']);
        exit();
    }
    
    // Validate required fields
    if (!is_array($data) || empty($data)) {
        http_response_code(400);
        echo json_encode(['error' => 'Data must be a non-empty array']);
        exit();
    }
    
    foreach ($data as $credential) {
        if (!isset($credential['id']) || !isset($credential['server']) || 
            !isset($credential['username']) || !isset($credential['password'])) {
            http_response_code(400);
            echo json_encode(['error' => 'Missing required fields']);
            exit();
        }
    }
    
    // Load existing credentials
    $existingCredentials = [];
    if (file_exists($credentialsFile)) {
        $existingData = file_get_contents($credentialsFile);
        if (!empty($existingData)) {
            $existingCredentials = json_decode($existingData, true);
            if (!is_array($existingCredentials)) {
                $existingCredentials = [];
            }
        }
    }
    
    // Merge new credentials with existing ones
    foreach ($data as $newCredential) {
        $found = false;
        foreach ($existingCredentials as &$existing) {
            if ($existing['server'] === $newCredential['server'] && 
                $existing['username'] === $newCredential['username']) {
                // Update existing credential
                $existing = $newCredential;
                $found = true;
                break;
            }
        }
        
        if (!$found) {
            // Add new credential
            $existingCredentials[] = $newCredential;
        }
    }
    
    // Save updated credentials
    $jsonData = json_encode($existingCredentials, JSON_PRETTY_PRINT);
    
    if (file_put_contents($credentialsFile, $jsonData) === false) {
        http_response_code(500);
        echo json_encode(['error' => 'Failed to save credentials']);
        exit();
    }
    
    http_response_code(200);
    echo json_encode(['success' => true, 'message' => 'Credentials saved successfully']);
    
} elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Return existing credentials
    if (file_exists($credentialsFile)) {
        $data = file_get_contents($credentialsFile);
        if (!empty($data)) {
            echo $data;
        } else {
            echo json_encode([]);
        }
    } else {
        echo json_encode([]);
    }
    
} else {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
}
?>


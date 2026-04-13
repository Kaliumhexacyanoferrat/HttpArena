<?php

//require 'Pgsql.php';

return match ($_SERVER['PATH_INFO']) {
    '/baseline11' => baseline(),
    '/baseline2'  => baseline(),
    //'/json'     => json(),
    //'/upload'     => upload(),
    '/pipeline'   => pipeline(),
    //'/async-db'   => asyncDb(),

    default => notFound()
};

// Init
//Pgsql::init();

function baseline()
{
    $sum = array_sum($_GET);
    if($_SERVER['REQUEST_METHOD'] === 'POST') {
        $sum += file_get_contents('php://input');
    }

    header('Content-Type: text/plain');
    echo $sum;
}

// function json()
// {
//     $total = [];
//     foreach (JSON_DATA as $item) {
//         $item['total'] = $item['price'] * $item['quantity'];
//         $total[] = $item;
//     }

//     header('Content-Type: application/json');
//     echo json_encode(['items' => $total, 'count' => count($total)],
//                     JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
// }

// function upload()
// {
//     header('Content-Type: text/plain');
//     echo strlen($_POST);
// }

function pipeline()
{
    header('Content-Type: text/plain');
    echo 'ok';
}

// function asyncDb()
// {
//     header('Content-Type: application/json');
//     echo Pgsql::query(
//         $_GET['min'] ?? 10,
//         $_GET['max'] ?? 50,
//         $_GET['limit'] ?? 50
//     );
// }

function notFound()
{
    header('Content-Type: text/plain', true, 404);
    echo 'Not Found';
}

var xAuthTokenHeaderName = 'x-auth-token';

angular.module('reservations', ['ngRoute', 'ngResource' , 'ngCookies'   ])
    .run(function ($rootScope, $http, $location, $cookieStore) {
        // setup code
    });


function ReservationController($scope, $http) {

    $scope.stompClient = null;
    $scope.reservations = [];


    $scope.alarm = function (reservation) {
        window.alert(JSON.stringify(reservation));
    };

    var init = function () {
        //        var socket = new SockJS('/spring-websocket-portfolio/portfolio');
        var notifications = '/notifications';
        var socket = new SockJS(notifications);
        var client = Stomp.over(socket);


        client.connect( {}, function (frame) {
            console.log('Connected ' + frame);
            var username = frame.headers['user-name'];
            client.subscribe("/topic/alarms"  , function (message) {
                $scope.alarm(JSON.parse(message.body));
            });
        }, function (error) {
            console.log("STOMP protocol error " + error);
        });


        // load the reservation table
        $http.get('/reservations').success(function (reservations) {
            $scope.reservations = reservations;
        });
    };


    init();

}


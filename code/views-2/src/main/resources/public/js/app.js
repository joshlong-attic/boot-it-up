
var xAuthTokenHeaderName = 'x-auth-token';

angular.module('reservations', ['ngRoute','ngResource' , 'ngCookies'   ])
    .run (function($rootScope, $http, $location, $cookieStore){

    });


function ReservationController ($scope , $http) {

    $scope.reservations = [] ;

    $scope.loadReservations = function(){
        $http.get('/reservations'  ).success(function (reservations) {
            $scope.reservations = reservations;
        });
    };


    $scope.loadReservations();

}

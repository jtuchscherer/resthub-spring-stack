/**
 * Routes
 */
(function($) {
	
	var app = $.sammy(function() {

		this.use(Sammy.Title);
		this.use(Sammy.Session);

		this.get('#/', function() {
			this.title('Login');
			$('#content').render('components/login.html', {})
		}, 'components/login.js');

		this.get('#/logout', function() {
			Alert('logout !');
		});

		this.get('#/register', function() {
			this.title('Register');
			$('#content').render('components/register.html', {});
		});

		/*
		 * Home page after authentication
		 */
		this.get('#/home', function(context) {
			this.title('Home');
			var user = this.session('user');
                         
                $('#header').render('components/header.html', {user: user});
			$('#content').listBookings({data: {user: user}, context: context});			
		}, 'components/booking/list.js');

		/*
		 * Search hotels
		 */
		this.get('#/hotel/search', function() {
			$('#result').listHotels({data: {searchVal: this.params['q']}});
		}, 'components/hotel/list.js');

		/*
		 * List bookings
		 */
		this.get('#/booking/list', function() {
			$('#content').listBookings();
		}, 'components/booking/list.js');

		/*
		 * List hotels
		 */
		this.get('#/hotel/list', function() {
			$('#result').listHotels();
		}, 'components/hotel/list.js');

		/**
		 * View hotel
		 */
		this.get('#/hotel/:id', function(context) {
			$('#content').viewHotel({data: {id: this.params['id']}, context: context});
        }, 'components/hotel/view.js');

		/**
		 * Book hotel identified by 'id'
		 */
		 this.get('#/booking/hotel/:id', function(context) {
			$('#content').bookBooking({id: this.params['id'], context: context});
        }, 'components/booking/book.js');
        
		/**
		 * Booking confirmation page
		 */
		this.get('#/booking/confirm', function() {
			$('div#booking-form-fields').confirmBooking();
		}, 'components/booking/confirm.js');

		/**
		 * Save booking in database
		 */
		this.post('#/booking', function() {
			alert('Not supported yet!');
		});

		/**
		 * User authentication
		 */
		this.post('#/user/check', function(context) {
			$('#header').login({context: context});
		});
	});

	$(function() {

		// Rebuild Lucene index
		$.ajax({
			url: 'api/lucene/rebuild',
			dataType: 'json',
			type: 'POST'
		});
		
                app.run('#/');

                bind('run-route', function() {
                    var user = this.session('user');
                    $('#header').render('components/header.html', {user: user});
                });

	});
})(jQuery);

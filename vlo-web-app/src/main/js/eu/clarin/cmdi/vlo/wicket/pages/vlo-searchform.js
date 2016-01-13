/* 
 * Copyright (C) 2015 CLARIN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

var interval;

function startSearch() {
    // after a short while start animating the search button
    var element = $('form#search #searchbutton');
    element.prop('disabled', true);

    // this will animate (fade in, fade out) the search button
    interval = setInterval(function () {
        element.animate({opacity:'-=0.5'}, 500);
        element.animate({opacity:'+=1'}, 500);
    }, 500); // delay between 'loops'
}

function endSearch() {
    // done searching, clean up
    var element = $('form#search #searchbutton');
    element.prop('disabled', false);
    
    // stop the animation
    if(interval !== null) { 
        clearInterval(interval);
    }
}
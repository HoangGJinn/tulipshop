/**
 * SHOPIFY SUPPLY STYLE - HORIZONTAL SCROLL EXPERIENCE
 * Horizontal scroll driven by vertical scrolling using GSAP Pinning
 */

(function() {
    'use strict';

    // ============================================
    // INITIALIZE LENIS SMOOTH SCROLL
    // ============================================
    const lenis = new Lenis({
        duration: 1.2,
        easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
        orientation: 'vertical',
        gestureOrientation: 'vertical',
        smoothWheel: true,
        wheelMultiplier: 1,
        smoothTouch: false,
        touchMultiplier: 2,
        infinite: false,
    });

    // Sync Lenis with GSAP ScrollTrigger
    lenis.on('scroll', ScrollTrigger.update);

    // Request Animation Frame loop for Lenis
    function raf(time) {
        lenis.raf(time);
        requestAnimationFrame(raf);
    }
    requestAnimationFrame(raf);

    // ============================================
    // REGISTER GSAP SCROLLTRIGGER PLUGIN
    // ============================================
    gsap.registerPlugin(ScrollTrigger);

    // Update ScrollTrigger when Lenis scrolls
    ScrollTrigger.scrollerProxy(document.body, {
        scrollTop(value) {
            if (arguments.length) {
                lenis.scrollTo(value, { immediate: true });
            }
            return lenis.scroll;
        },
        getBoundingClientRect() {
            return {
                top: 0,
                left: 0,
                width: window.innerWidth,
                height: window.innerHeight
            };
        },
        pinType: document.body.style.transform ? "transform" : "fixed"
    });

    // ============================================
    // HORIZONTAL SCROLL ANIMATION
    // ============================================
    function initHorizontalScroll() {
        const mainContainer = document.getElementById('main-container');
        const heroSection = document.querySelector('.hero-section');
        const heroText = document.querySelectorAll('.layer-text .hero-title');
        
        // Get individual images for scatter animation
        const imageBack1 = document.querySelector('.image-back-1');
        const imageBack2 = document.querySelector('.image-back-2');
        const imageBack3 = document.querySelector('.image-back-3');
        const imageFront1 = document.querySelector('.image-front-1');
        const imageFront2 = document.querySelector('.image-front-2');
        const imageFront3 = document.querySelector('.image-front-3');
        
        if (!mainContainer || !heroSection) return;

        // Calculate scroll distance
        const scrollDistance = mainContainer.offsetWidth - window.innerWidth;

        // Create master timeline for horizontal movement with pinning
        const mainTL = gsap.timeline({
            scrollTrigger: {
                trigger: mainContainer,
                start: 'top top',
                end: () => `+=${scrollDistance}`,
                pin: true,
                scrub: 1,
                anticipatePin: 1,
                invalidateOnRefresh: true,
                id: 'horizontal-scroll'
            }
        });

        // Phase 1: The Explosion (0% to 20% of timeline)
        // Images scatter from center to their final positions
        if (imageBack1) {
            mainTL.to(imageBack1, {
                x: '-30vw',
                y: '-25vh',
                rotation: -10,
                duration: 0.2, // 20% of timeline
                ease: 'power2.out'
            }, 0);
        }

        if (imageBack2) {
            mainTL.to(imageBack2, {
                x: '35vw',
                y: '-15vh',
                rotation: 5,
                duration: 0.2,
                ease: 'power2.out'
            }, 0);
        }

        if (imageBack3) {
            mainTL.to(imageBack3, {
                x: '-20vw',
                y: '30vh',
                rotation: -5,
                duration: 0.2,
                ease: 'power2.out'
            }, 0);
        }

        if (imageFront1) {
            mainTL.to(imageFront1, {
                x: '25vw',
                y: '25vh',
                rotation: 10,
                duration: 0.2,
                ease: 'power2.out'
            }, 0);
        }

        if (imageFront2) {
            mainTL.to(imageFront2, {
                x: '-25vw',
                y: '20vh',
                rotation: -8,
                duration: 0.2,
                ease: 'power2.out'
            }, 0);
        }

        if (imageFront3) {
            mainTL.to(imageFront3, {
                x: '30vw',
                y: '-20vh',
                rotation: 8,
                duration: 0.2,
                ease: 'power2.out'
            }, 0);
        }

        // Phase 2: Horizontal Slide (entire timeline, starting from 0%)
        // Move container left as user scrolls down
        mainTL.to(mainContainer, {
            x: () => {
                return -(mainContainer.offsetWidth - window.innerWidth);
            },
            ease: 'none',
            duration: 1
        }, 0);
    }

    // ============================================
    // PRODUCT HOTSPOT INTERACTIONS
    // ============================================
    function initProductHotspots() {
        // Lifestyle hotspot interactions
        const lifestyleHotspots = document.querySelectorAll('.hotspot-lifestyle');
        
        lifestyleHotspots.forEach((hotspot) => {
            hotspot.addEventListener('click', () => {
                const glassCard = hotspot.closest('.lifestyle-col').querySelector('.glass-card');
                if (glassCard) {
                    // Scroll glass card into view or trigger quick view
                    gsap.to(glassCard, {
                        scale: 1.05,
                        duration: 0.3,
                        yoyo: true,
                        repeat: 1
                    });
                }
            });
        });

        // Quick view button handlers
        const quickViewBtns = document.querySelectorAll('.quick-view-btn-showcase');
        quickViewBtns.forEach((btn) => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const productSlide = btn.closest('.product-slide');
                const productName = productSlide.querySelector('.product-title').textContent;
                console.log('Quick view:', productName);
                // Add your quick view logic here
            });
        });

        // Glass card interactions
        const glassCards = document.querySelectorAll('.glass-card');
        glassCards.forEach((card) => {
            card.addEventListener('click', () => {
                const productSlide = card.closest('.product-slide');
                const productName = productSlide.querySelector('.glass-card-title').textContent;
                console.log('View product:', productName);
                // Add navigation logic here
            });
        });
    }

    // ============================================
    // PRODUCT SLIDE ANIMATIONS
    // ============================================
    function initProductAnimations() {
        const productSlides = document.querySelectorAll('.product-slide');
        const horizontalScroll = ScrollTrigger.getById('horizontal-scroll');
        
        if (!horizontalScroll) return;
        
        productSlides.forEach((slide, index) => {
            // Staggered entrance animation as slides come into view
            const cutout = slide.querySelector('.product-cutout-wrapper');
            const glassCard = slide.querySelector('.glass-card');
            
            if (cutout) {
                gsap.from(cutout, {
                    scale: 0.8,
                    opacity: 0,
                    duration: 1,
                    ease: 'power3.out',
                    scrollTrigger: {
                        trigger: slide,
                        start: 'left 80%',
                        end: 'left 50%',
                        toggleActions: 'play none none reverse',
                        containerAnimation: horizontalScroll
                    }
                });
            }
            
            if (glassCard) {
                gsap.from(glassCard, {
                    y: 50,
                    opacity: 0,
                    duration: 1,
                    ease: 'power3.out',
                    scrollTrigger: {
                        trigger: slide,
                        start: 'left 80%',
                        end: 'left 50%',
                        toggleActions: 'play none none reverse',
                        containerAnimation: horizontalScroll
                    },
                    delay: 0.2
                });
            }
        });
    }

    // ============================================
    // HEADER SCROLL EFFECT
    // ============================================
    function initHeaderScroll() {
        const header = document.querySelector('.main-header');
        if (!header) return;
        
        // Keep header visible during horizontal scroll
        gsap.to(header, {
            opacity: 1,
            scrollTrigger: {
                trigger: document.body,
                start: 'top top',
                end: 'bottom bottom',
                scrub: false
            }
        });
    }

    // ============================================
    // IMAGE LAZY LOADING ENHANCEMENT
    // ============================================
    function initLazyLoading() {
        const images = document.querySelectorAll('img[loading="lazy"]');
        
        if ('IntersectionObserver' in window) {
            const imageObserver = new IntersectionObserver((entries, observer) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const img = entry.target;
                        img.classList.add('loaded');
                        observer.unobserve(img);
                    }
                });
            }, {
                rootMargin: '50px' // Load images slightly before they're visible
            });

            images.forEach(img => imageObserver.observe(img));
        }
    }

    // ============================================
    // PERFORMANCE OPTIMIZATION
    // ============================================
    function optimizePerformance() {
        // Reduce motion for users who prefer it
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            lenis.options.duration = 0;
            lenis.options.smoothWheel = false;
            
            // Disable animations for reduced motion
            gsap.set('.hero-title, .hero-image', { clearProps: 'all' });
        }

        // Throttle resize events
        let resizeTimer;
        window.addEventListener('resize', () => {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(() => {
                ScrollTrigger.refresh();
            }, 250);
        });
    }

    // ============================================
    // INITIALIZATION
    // ============================================
    function init() {
        // Wait for DOM to be ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', init);
            return;
        }

        // Initialize all features
        initHorizontalScroll();
        initProductHotspots();
        initProductAnimations();
        initHeaderScroll();
        initLazyLoading();
        optimizePerformance();

        // Refresh ScrollTrigger after a short delay to ensure all elements are rendered
        setTimeout(() => {
            ScrollTrigger.refresh();
        }, 500);

        console.log('Tulip Shop - Horizontal Scroll Experience initialized');
    }

    // Start initialization
    init();

    // Expose Lenis instance globally for debugging (optional)
    window.lenis = lenis;

})();

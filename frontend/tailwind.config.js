/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        sus: {
          green: '#009C3B',
          blue: '#009EE3',
        },
      },
    },
  },
  plugins: [],
};

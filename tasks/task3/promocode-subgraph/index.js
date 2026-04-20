import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import { buildSubgraphSchema } from '@apollo/subgraph';
import gql from 'graphql-tag';

const typeDefs = gql`
  schema
    @link(
      url: "https://specs.apollo.dev/federation/v2.3",
      import: ["@key", "@external", "@override", "@requires"]
    ) {
    query: Query
  }

  extend type Booking @key(fields: "id") {
    id: ID! @external
    promoCode: String @external
    discountPercent: Float! @override(from: "booking")
    discountInfo: DiscountInfo @requires(fields: "promoCode")
  }

  type DiscountInfo {
    isValid: Boolean!
    originalDiscount: Float!
    finalDiscount: Float!
    description: String
    expiresAt: String
    applicableHotels: [ID!]!
  }

  type Query {
    validatePromoCode(code: String!, hotelId: ID): DiscountInfo!
    activePromoCodes: [DiscountInfo!]!
  }
`;

const resolvers = {
  Booking: {
    discountPercent: (booking) => {
      // переопределяем значение из booking-subgraph
      if (booking.promoCode === 'SUMMER') {
        return 25;
      }
      if (booking.promoCode === 'VIP') {
        return 40;
      }
      return booking.discountPercent ?? 0;
    },

    discountInfo: (booking) => {
      if (!booking.promoCode) {
        return {
          isValid: false,
          originalDiscount: booking.discountPercent ?? 0,
          finalDiscount: booking.discountPercent ?? 0,
          description: 'No promo code',
          expiresAt: null,
          applicableHotels: []
        };
      }

      if (booking.promoCode === 'SUMMER') {
        return {
          isValid: true,
          originalDiscount: booking.discountPercent ?? 10,
          finalDiscount: 25,
          description: 'Summer promo',
          expiresAt: '2025-12-31',
          applicableHotels: ['h1', 'h2']
        };
      }

      if (booking.promoCode === 'VIP') {
        return {
          isValid: true,
          originalDiscount: booking.discountPercent ?? 15,
          finalDiscount: 40,
          description: 'VIP exclusive promo',
          expiresAt: '2025-12-31',
          applicableHotels: ['h1']
        };
      }

      return {
        isValid: false,
        originalDiscount: booking.discountPercent ?? 0,
        finalDiscount: booking.discountPercent ?? 0,
        description: 'Invalid promo',
        expiresAt: null,
        applicableHotels: []
      };
    }
  },

  Query: {
    validatePromoCode: (_, { code }) => {
      if (code === 'SUMMER') {
        return {
          isValid: true,
          originalDiscount: 10,
          finalDiscount: 25,
          description: 'Summer promo',
          expiresAt: '2025-12-31',
          applicableHotels: ['h1', 'h2']
        };
      }

      if (code === 'VIP') {
        return {
          isValid: true,
          originalDiscount: 15,
          finalDiscount: 40,
          description: 'VIP promo',
          expiresAt: '2025-12-31',
          applicableHotels: ['h1']
        };
      }

      return {
        isValid: false,
        originalDiscount: 0,
        finalDiscount: 0,
        description: 'Invalid promo',
        expiresAt: null,
        applicableHotels: []
      };
    },

    activePromoCodes: () => [
      {
        isValid: true,
        originalDiscount: 10,
        finalDiscount: 25,
        description: 'Summer promo',
        expiresAt: '2025-12-31',
        applicableHotels: ['h1', 'h2']
      },
      {
        isValid: true,
        originalDiscount: 15,
        finalDiscount: 40,
        description: 'VIP promo',
        expiresAt: '2025-12-31',
        applicableHotels: ['h1']
      }
    ]
  }
};

const server = new ApolloServer({
  schema: buildSubgraphSchema([{ typeDefs, resolvers }]),
});

startStandaloneServer(server, {
  listen: { port: 4003 },
}).then(() => {
  console.log('✅ Promocode subgraph ready at http://localhost:4003/');
});
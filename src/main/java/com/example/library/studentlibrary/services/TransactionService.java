package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        // If it fails: throw new Exception("Book is either unavailable or not present");
        //2. card is present and activated
        // If it fails: throw new Exception("Card is invalid");
        //3. number of books issued against the card is strictly less than max_allowed_books
        // If it fails: throw new Exception("Book limit has reached for this card");
        //If the transaction is successful, save the transaction to the list of transactions and return the id

        //Note that the error message should match exactly in all cases
        Book book = bookRepository5.findById(bookId).get() ;
        Card card = cardRepository5.findById(cardId).get() ;
        Transaction transaction = new Transaction() ;
        transaction.setTransactionStatus(TransactionStatus.FAILED);

        if( book != null && book.isAvailable()){
            if(card != null && card.getCardStatus() == CardStatus.ACTIVATED){
                if(card.getBooks().size() < max_allowed_books){

                    transaction.setBook(book);
                    transaction.setTransactionDate(new Date());
                    transaction.setCard(card);
                    transaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);
                    transaction.setIssueOperation(true);
                    transaction.setFineAmount(0);

                    transactionRepository5.save(transaction);
                    //updating card
                    List<Book> books = card.getBooks() ;
                    books.add(book) ;
                    card.setBooks(books);
                    cardRepository5.save(card) ;
                    //updating the book
                    book.setCard(card);
                    List<Transaction> transactions = book.getTransactions() ;
                    transactions.add(transaction) ;
                    book.setTransactions(transactions) ;
                    book.setAvailable(false);
                    bookRepository5.updateBook(book) ;

                }else {
                    throw new Exception("Book limit has reached for this card") ;
                }
            }else {
                throw new Exception("Card is invalid") ;
            }
        }else {
            throw new Exception("Book is either unavailable or not present") ;
        }
        return transaction.getTransactionId(); //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        Card card=cardRepository5.findById(cardId).get();
        Book book=bookRepository5.findById(bookId).get();
        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //  SimpleDateFormat obj = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss") ;

        Date issueDate = transaction.getTransactionDate() ;
        Date curentDate = new Date() ;
        long difference = curentDate.getTime() - issueDate.getTime() ;
        long daysKept = (difference/(1000*60*60*24)) % 365 ; //assuming its < 365

        long fine = (daysKept - getMax_allowed_days) * fine_per_day ;

        //make the book available for other users
        book.setAvailable(true);
        bookRepository5.updateBook(book);
        //make a new transaction for return book which contains the fine amount as well
        Transaction returnBookTransaction = new Transaction() ;
        if(fine > 0){
            returnBookTransaction.setFineAmount((int)fine);
        }
        returnBookTransaction.setTransactionDate(new Date());
        returnBookTransaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);
        returnBookTransaction.setBook(book);
        returnBookTransaction.setCard(card);
        returnBookTransaction.setIssueOperation(true);

        transactionRepository5.save(transaction) ;

        return returnBookTransaction; //return the transaction after updating all details
    }
}